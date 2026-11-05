package com.dotlo.line4thon_18team.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.PlaceMatcher;
import com.dotlo.line4thon_18team.place.PlaceMatcher.Candidate;
import com.dotlo.line4thon_18team.place.PlaceMatcher.MatchResult;
import com.dotlo.line4thon_18team.place.PlaceMatcher.MatchStatus;
import com.dotlo.line4thon_18team.place.PlaceMatcher.PlaceInput;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import com.dotlo.line4thon_18team.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

// 매칭 결과 → 저장 장소 상태. 구글·DB 없이 PlaceMatcher 결과를 직접 넣어서 확인
class PlaceEnrichServiceTest {

    private final TripService tripService = mock(TripService.class);
    private final PlaceMatcher placeMatcher = mock(PlaceMatcher.class);
    private final SavedPlaceRepository repository = mock(SavedPlaceRepository.class);
    private final PlaceEnrichService service = new PlaceEnrichService(tripService, placeMatcher, repository,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));
    private Trip trip;

    @BeforeEach
    void setUp() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        trip = Trip.create(User.builder().googleSub("s").email("a@b.c").name("n").build(),
                "오사카", start, start.plusDays(2), Companion.ALONE, null);
        when(tripService.findOwnedTrip(1L, 7L)).thenReturn(trip);
    }

    @Test
    void 매칭_결과에_따라_장소_정보와_상태를_채운다() {
        SavedPlace ichiran = place(1L, "이치란", "리스트");
        SavedPlace closed = place(2L, "폐업한 가게", "리스트");
        SavedPlace himeji = place(3L, "히메지성", "리스트");
        SavedPlace review = place(4L, "이치란 라멘", "리스트");
        SavedPlace notFound = place(5L, "ㅁㄴㅇㄹ", "리스트");
        pending(ichiran, closed, himeji, review, notFound);
        googleReturns(Map.of(
                1L, matched(1L, new Candidate("p1", "이치란 우메다", "오사카", 34.70, 135.50), PlaceCategory.FOOD, false),
                2L, matched(2L, new Candidate("p2", "폐업한 가게", "오사카", 34.69, 135.50), PlaceCategory.CAFE, true),
                3L, matched(3L, new Candidate("p3", "히메지성", "히메지", 34.84, 134.69), PlaceCategory.CULTURE, false),  // 오사카에서 약 75km
                4L, new MatchResult(4L, MatchStatus.NEEDS_REVIEW, null, List.of(), null, false),
                5L, new MatchResult(5L, MatchStatus.NOT_FOUND, null, List.of(), null, false)));

        service.enrich(1L, 7L);

        assertThat(ichiran.getMatchStatus()).isEqualTo(PlaceMatchStatus.MATCHED);
        assertThat(ichiran.getPlaceId()).isEqualTo("p1");
        assertThat(ichiran.getAddress()).isEqualTo("오사카");
        assertThat(ichiran.getCategory()).isEqualTo(PlaceCategory.FOOD);
        assertThat(closed.getMatchStatus()).isEqualTo(PlaceMatchStatus.CLOSED);
        assertThat(himeji.getMatchStatus()).isEqualTo(PlaceMatchStatus.OUT_OF_CITY);
        assertThat(himeji.getCategory()).isEqualTo(PlaceCategory.CULTURE);
        assertThat(review.getMatchStatus()).isEqualTo(PlaceMatchStatus.NEEDS_REVIEW);
        assertThat(review.getPlaceId()).isNull();
        assertThat(notFound.getMatchStatus()).isEqualTo(PlaceMatchStatus.NOT_FOUND);
    }

    @Test
    void 구글_오류가_나면_앞_장소_결과는_남고_남은_장소는_PENDING이다() {
        SavedPlace first = place(1L, "이치란", "리스트");
        SavedPlace second = place(2L, "하루카스", "리스트");
        pending(first, second);
        when(placeMatcher.matchPlaces(any(), anyDouble(), anyDouble()))
                .thenReturn(List.of(matched(1L, new Candidate("p1", "이치란", "오사카", 34.70, 135.50), PlaceCategory.FOOD, false)))
                .thenThrow(new RestClientException("quota"));

        assertThatThrownBy(() -> service.enrich(1L, 7L)).isInstanceOf(RestClientException.class);

        assertThat(first.getMatchStatus()).isEqualTo(PlaceMatchStatus.MATCHED);
        assertThat(second.getMatchStatus()).isEqualTo(PlaceMatchStatus.PENDING);
    }

    @Test
    void 같은_placeId가_이미_있으면_리스트_이름을_합치고_이_행은_지운다() {
        SavedPlace existing = place(1L, "이치란", "오사카 맛집");
        SavedPlace pin = place(2L, "이치란 핀", "라멘");
        pending(pin);
        when(repository.findFirstByTripIdAndPlaceId(7L, "p1")).thenReturn(Optional.of(existing));
        googleReturns(Map.of(
                2L, matched(2L, new Candidate("p1", "이치란", "오사카", 34.70, 135.50), PlaceCategory.FOOD, false)));

        service.enrich(1L, 7L);

        assertThat(existing.getListNames()).containsExactlyInAnyOrder("오사카 맛집", "라멘");
        verify(repository).delete(pin);
    }

    @Test
    void 보강이_두_번_동시에_돌아_이미_처리된_장소면_건드리지_않는다() {
        SavedPlace ichiran = place(1L, "이치란", "오사카 맛집");
        pending(ichiran);
        // 다른 요청이 먼저 이 장소를 매칭해서 저장한 상태
        ichiran.applyMatch("p1", "오사카", 34.70, 135.50, PlaceCategory.FOOD, PlaceMatchStatus.MATCHED);
        when(repository.findFirstByTripIdAndPlaceId(7L, "p1")).thenReturn(Optional.of(ichiran));
        googleReturns(Map.of(
                1L, matched(1L, new Candidate("p1", "이치란", "오사카", 34.70, 135.50), PlaceCategory.FOOD, false)));

        service.enrich(1L, 7L);

        verify(repository, never()).delete(any());
        assertThat(ichiran.getMatchStatus()).isEqualTo(PlaceMatchStatus.MATCHED);
    }

    private SavedPlace place(Long id, String name, String listName) {
        SavedPlace p = SavedPlace.fromTakeout(trip, null, name, null, null, null, null, listName);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private void pending(SavedPlace... places) {
        when(repository.findAllByTripIdAndMatchStatus(7L, PlaceMatchStatus.PENDING)).thenReturn(List.of(places));
        for (SavedPlace p : places) when(repository.findById(p.getId())).thenReturn(Optional.of(p));
    }

    // 장소 1개씩 매칭을 부르므로, 들어온 장소 id 로 결과를 골라 돌려준다
    private void googleReturns(Map<Long, MatchResult> byId) {
        when(placeMatcher.matchPlaces(any(), anyDouble(), anyDouble())).thenAnswer(inv -> {
            List<PlaceInput> inputs = inv.getArgument(0);
            return List.of(byId.get(inputs.get(0).id()));
        });
    }

    private static MatchResult matched(Long id, Candidate c, PlaceCategory category, boolean closed) {
        return new MatchResult(id, MatchStatus.MATCHED, c, List.of(), category, closed);
    }
}
