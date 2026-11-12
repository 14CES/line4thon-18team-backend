package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.itinerary.edit.ItineraryDiff;
import com.dotlo.line4thon_18team.itinerary.edit.dto.ItineraryPlaceAddResult;
import com.dotlo.line4thon_18team.itinerary.validation.model.DayDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.VisitDraft;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures;
import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;
import com.dotlo.line4thon_18team.itinerary.version.entity.ItinerarySource;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.place.service.PlaceEnrichService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 장소 하나 일정에 넣기 (COLLAB-ADD-001): 저장 장소에 담기 → 상태 확인 → AI가 자리 정하기 → 새 버전 저장.
 * AI가 어느 날 어디에 넣는지는 {@link ItineraryEditServiceTest}에서 본다.
 */
class ItineraryPlaceAddServiceTest {

    private static final long TRIP = 1L;
    private static final long USER = 7L;
    private static final String DOTONBORI = "place-dotonbori";
    private static final String KUROMON = "place-kuromon";
    private static final String SPA = "place-spa";
    private static final ValidationResult VALID = ValidationResult.of(List.of(), List.of());

    private final PlaceEnrichService enrichService = Mockito.mock(PlaceEnrichService.class);
    private final SavedPlaceRepository savedPlaces = Mockito.mock(SavedPlaceRepository.class);
    private final ItineraryService itineraryService = Mockito.mock(ItineraryService.class);
    private final ItineraryEditService editService = Mockito.mock(ItineraryEditService.class);

    private final ItineraryPlaceAddService service = new ItineraryPlaceAddService(
            enrichService, savedPlaces, itineraryService, editService);

    @Test
    void 장소를_넣고_새_버전으로_저장한다() {
        givenSaved(SPA, "스파월드", PlaceMatchStatus.MATCHED);
        givenCurrent(3, twoDays());
        givenEdited(withSpa(), "둘째 날 마지막에 스파월드를 넣었습니다.");
        givenSaveReturns(4);

        ItineraryPlaceAddResult result = service.add(TRIP, USER, SPA, null);

        assertThat(result.placeName()).isEqualTo("스파월드");
        assertThat(result.reason()).isEqualTo("둘째 날 마지막에 스파월드를 넣었습니다.");
        assertThat(result.itinerary().version()).isEqualTo(4);
        assertThat(result.diff().changesOf(ItineraryDiff.ChangeKind.ADDED))
                .extracting(ItineraryDiff.ItemChange::placeId)
                .containsExactly(SPA);
        Mockito.verify(itineraryService).save(TRIP, USER, ItinerarySource.AI_EDITED, 3, withSpa(), VALID);
    }

    @Test
    void 저장_장소에_담은_뒤에_일정을_읽는다() {
        // 구글 조회가 끝나야 좌표·유형이 생기고, 그제야 검증기가 그 장소를 쓸 수 있다
        givenSaved(SPA, "스파월드", PlaceMatchStatus.MATCHED);
        givenCurrent(3, twoDays());
        givenEdited(withSpa(), "넣었습니다.");
        givenSaveReturns(4);

        service.add(TRIP, USER, SPA, null);

        InOrder order = Mockito.inOrder(enrichService, savedPlaces, itineraryService);
        order.verify(enrichService).add(USER, TRIP, SPA);
        order.verify(savedPlaces).findFirstByTripIdAndPlaceId(TRIP, SPA);
        order.verify(itineraryService).getLatest(TRIP, USER);
    }

    @Test
    void 폐업한_장소는_400이고_AI를_부르지_않는다() {
        givenSaved(SPA, "스파월드", PlaceMatchStatus.CLOSED);

        assertThatThrownBy(() -> service.add(TRIP, USER, SPA, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_NOT_PLANNABLE)
                .hasMessageContaining("폐업");

        Mockito.verifyNoInteractions(editService);
    }

    @Test
    void 여행_도시에서_먼_장소도_400이다() {
        givenSaved(SPA, "스파월드", PlaceMatchStatus.OUT_OF_CITY);

        assertThatThrownBy(() -> service.add(TRIP, USER, SPA, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_NOT_PLANNABLE)
                .hasMessageContaining("떨어진");

        Mockito.verifyNoInteractions(editService);
    }

    @Test
    void 확인_필요_장소도_400이다() {
        // 사용자가 후보를 아직 고르지 않은 장소. 검증기가 좌표를 모르니 일정에 넣을 수 없다
        givenSaved(SPA, "스파월드", PlaceMatchStatus.NEEDS_REVIEW);

        assertThatThrownBy(() -> service.add(TRIP, USER, SPA, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_NOT_PLANNABLE);

        Mockito.verifyNoInteractions(editService);
    }

    @Test
    void 이미_일정에_있는_장소는_400이다() {
        givenSaved(DOTONBORI, "도톤보리", PlaceMatchStatus.MATCHED);
        givenCurrent(3, twoDays());

        assertThatThrownBy(() -> service.add(TRIP, USER, DOTONBORI, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_ALREADY_IN_ITINERARY);

        Mockito.verifyNoInteractions(editService);
    }

    @Test
    void AI가_넣지_못하면_같은_일정을_새_버전으로_쌓지_않는다() {
        givenSaved(SPA, "스파월드", PlaceMatchStatus.MATCHED);
        givenCurrent(3, twoDays());
        givenEdited(twoDays(), "하루에 넣는 개수가 꽉 차서 넣지 못했습니다.");

        assertThatThrownBy(() -> service.add(TRIP, USER, SPA, null))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PLACE_ADD_FAILED)
                .hasMessageContaining("넣지 못했습니다");

        Mockito.verify(itineraryService, Mockito.never()).save(Mockito.anyLong(), Mockito.anyLong(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void 사용자_요청을_수정_요청_문장에_붙인다() {
        givenSaved(SPA, "스파월드", PlaceMatchStatus.MATCHED);
        givenCurrent(3, twoDays());
        givenEdited(withSpa(), "넣었습니다.");
        givenSaveReturns(4);

        service.add(TRIP, USER, SPA, "  저녁에 가고 싶어요  ");

        ArgumentCaptor<String> request = ArgumentCaptor.forClass(String.class);
        Mockito.verify(editService).edit(Mockito.eq(TRIP), Mockito.eq(USER), Mockito.any(), request.capture());
        assertThat(request.getValue()).contains("스파월드").contains("저녁에 가고 싶어요");
    }

    // ---- given ----

    private void givenSaved(String placeId, String name, PlaceMatchStatus status) {
        SavedPlace place = Mockito.mock(SavedPlace.class);
        Mockito.when(place.getName()).thenReturn(name);
        Mockito.when(place.getMatchStatus()).thenReturn(status);
        Mockito.when(savedPlaces.findFirstByTripIdAndPlaceId(TRIP, placeId)).thenReturn(Optional.of(place));
    }

    private void givenCurrent(int version, ItineraryDraft draft) {
        Mockito.when(itineraryService.getLatest(TRIP, USER)).thenReturn(
                new ItineraryVersionView(version, ItinerarySource.AI_GENERATED, null, USER,
                        LocalDateTime.of(2026, 12, 1, 10, 0), draft, "{}"));
    }

    private void givenEdited(ItineraryDraft draft, String reason) {
        Mockito.when(editService.edit(Mockito.eq(TRIP), Mockito.eq(USER), Mockito.any(), Mockito.anyString()))
                .thenReturn(new EditedItinerary(draft, VALID, reason, 1));
    }

    private void givenSaveReturns(int version) {
        Mockito.when(itineraryService.save(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(call -> new ItineraryVersionView(version, ItinerarySource.AI_EDITED, version - 1, USER,
                        LocalDateTime.of(2026, 12, 1, 11, 0), call.getArgument(4), "{}"));
    }

    private static ItineraryDraft twoDays() {
        return ItineraryDraft.of(
                DayDraft.of(OsakaFixtures.DAY1, VisitDraft.of("v-aaa", DOTONBORI)),
                DayDraft.of(OsakaFixtures.DAY2, VisitDraft.of("v-bbb", KUROMON)));
    }

    private static ItineraryDraft withSpa() {
        return ItineraryDraft.of(
                DayDraft.of(OsakaFixtures.DAY1, VisitDraft.of("v-aaa", DOTONBORI)),
                DayDraft.of(OsakaFixtures.DAY2, VisitDraft.of("v-bbb", KUROMON), VisitDraft.of("v-spa", SPA)));
    }
}
