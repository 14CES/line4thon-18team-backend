package com.dotlo.line4thon_18team.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.PlaceMatcher;
import com.dotlo.line4thon_18team.place.PlaceMatcher.Candidate;
import com.dotlo.line4thon_18team.place.PlaceMatcher.MatchResult;
import com.dotlo.line4thon_18team.place.PlaceMatcher.MatchStatus;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import com.dotlo.line4thon_18team.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// 매칭 결과 → 저장 장소 상태. 구글·DB 없이 PlaceMatcher 결과를 직접 넣어서 확인
class PlaceEnrichServiceTest {

    private final TripService tripService = mock(TripService.class);
    private final PlaceMatcher placeMatcher = mock(PlaceMatcher.class);
    private final SavedPlaceRepository repository = mock(SavedPlaceRepository.class);
    private final PlaceEnrichService service = new PlaceEnrichService(tripService, placeMatcher, repository);

    @Test
    void 매칭_결과에_따라_장소_정보와_상태를_채운다() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        Trip trip = Trip.create(User.builder().googleSub("s").email("a@b.c").name("n").build(),
                "오사카", start, start.plusDays(2), Companion.ALONE, null);
        when(tripService.findOwnedTrip(1L, 7L)).thenReturn(trip);

        SavedPlace ichiran = place(trip, 1L, "이치란");
        SavedPlace closed = place(trip, 2L, "폐업한 가게");
        SavedPlace himeji = place(trip, 3L, "히메지성");
        SavedPlace review = place(trip, 4L, "이치란 라멘");
        SavedPlace notFound = place(trip, 5L, "ㅁㄴㅇㄹ");
        when(repository.findAllByTripIdAndMatchStatus(7L, PlaceMatchStatus.PENDING))
                .thenReturn(List.of(ichiran, closed, himeji, review, notFound));

        when(placeMatcher.matchPlaces(any(), anyDouble(), anyDouble())).thenReturn(List.of(
                matched(1L, new Candidate("p1", "이치란 우메다", "오사카", 34.70, 135.50), PlaceCategory.FOOD, false),
                matched(2L, new Candidate("p2", "폐업한 가게", "오사카", 34.69, 135.50), PlaceCategory.CAFE, true),
                matched(3L, new Candidate("p3", "히메지성", "히메지", 34.84, 134.69), PlaceCategory.CULTURE, false),  // 오사카에서 약 75km
                new MatchResult(4L, MatchStatus.NEEDS_REVIEW, null, List.of(), null, false),
                new MatchResult(5L, MatchStatus.NOT_FOUND, null, List.of(), null, false)));

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

    private static SavedPlace place(Trip trip, Long id, String name) {
        SavedPlace p = SavedPlace.fromTakeout(trip, null, name, null, null, null, null, "리스트");
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private static MatchResult matched(Long id, Candidate c, PlaceCategory category, boolean closed) {
        return new MatchResult(id, MatchStatus.MATCHED, c, List.of(), category, closed);
    }
}
