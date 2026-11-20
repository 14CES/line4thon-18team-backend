package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.llm.FakeLlmClient;
import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.global.llm.schema.SchemaLoader;
import com.dotlo.line4thon_18team.itinerary.ItineraryValidationService.ValidationRun;
import com.dotlo.line4thon_18team.itinerary.ai.PlanGenerationTask;
import com.dotlo.line4thon_18team.itinerary.validation.ItineraryValidator;
import com.dotlo.line4thon_18team.itinerary.validation.ValidationPolicy;
import com.dotlo.line4thon_18team.itinerary.validation.model.OpeningHours;
import com.dotlo.line4thon_18team.itinerary.validation.model.PlaceInfo;
import com.dotlo.line4thon_18team.itinerary.validation.result.IssueCode;
import com.dotlo.line4thon_18team.itinerary.validation.result.IssueKind;
import com.dotlo.line4thon_18team.itinerary.validation.result.Severity;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationIssue;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.itinerary.validation.support.FakePlaceInfoProvider;
import com.dotlo.line4thon_18team.itinerary.validation.support.FakeTravelTimeProvider;
import com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures;
import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;
import com.dotlo.line4thon_18team.itinerary.version.entity.ItinerarySource;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.DOTONBORI;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.KUROMON;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.OSAKA_CASTLE;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.UMEDA_SKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI 일정 생성. 모델 응답은 가짜로 주고, 검증기는 실제 오사카 장소로 돌린다.
 */
class ItineraryGenerationServiceTest {

    private final FakeLlmClient llm = new FakeLlmClient();
    private final ItineraryValidationService validationService = Mockito.mock(ItineraryValidationService.class);
    private final ItineraryService itineraryService = Mockito.mock(ItineraryService.class);
    private final ItineraryGenerationService service = new ItineraryGenerationService(
            validationService,
            new StructuredLlmExecutor(llm, 3, 0, millis -> { }),
            new PlanGenerationTask(new SchemaLoader()),
            itineraryService);

    @Test
    void 장소를_일차별로_나누고_시각을_계산한다() {
        given(matched(DOTONBORI), matched(KUROMON), matched(OSAKA_CASTLE));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1, 2]},
                          {"dayNumber": 2, "placeNumbers": [3]},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(result.attempts()).isEqualTo(1);
        assertThat(result.draft().days()).hasSize(4);
        assertThat(result.draft().days().get(0).visits()).extracting(v -> v.placeId())
                .containsExactly(DOTONBORI.placeId(), KUROMON.placeId());
        assertThat(result.validation().days().get(0).visits()).hasSize(2);
        assertThat(result.validation().summary().valid()).isTrue();
    }

    @Test
    void itemId를_새로_만들고_서로_겹치지_않는다() {
        given(matched(DOTONBORI), matched(KUROMON));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": [2]},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        GeneratedItinerary result = service.generate(1L, 7L);

        List<String> itemIds = result.draft().days().stream()
                .flatMap(day -> day.visits().stream())
                .map(visit -> visit.itemId())
                .toList();
        assertThat(itemIds).hasSize(2).doesNotHaveDuplicates().allMatch(id -> id.startsWith("v-"));
    }

    @Test
    void 넣지_못한_장소는_place_id로_돌려준다() {
        given(matched(DOTONBORI), matched(KUROMON), matched(UMEDA_SKY));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": [2]},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": [3]}
                """);

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(result.excludedPlaceIds()).containsExactly(UMEDA_SKY.placeId());
    }

    @Test
    void 검증에_걸리면_문제를_알려주고_다시_배치한다() {
        givenUnrepairable(matched(DOTONBORI), matched(OsakaFixtures.CAFE_CLOSED_EVERY_DAY));
        // 여행 내내 휴무인 카페를 넣음. 서버가 고쳐도 나아지지 않아서 AI에게 다시 배치를 맡긴다
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1, 2]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": [2]}
                """);

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(result.attempts()).isEqualTo(2);
        assertThat(result.validation().summary().valid()).isTrue();
        assertThat(llm.requests().get(1).messages().get(1).content())
                .contains("지난번 배치에서 아래 문제가 있었습니다")
                .contains("그날 휴무입니다");
    }

    @Test
    void 옮겨서_풀리는_문제는_서버가_고치고_AI를_다시_부르지_않는다() {
        given(matched(DOTONBORI), matched(OsakaFixtures.CAFE_CLOSED_MONDAY));
        // 1일차(월요일)에 휴무인 카페를 넣음 → 서버가 다른 날로 옮긴다
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1, 2]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(llm.callCount()).isEqualTo(1);
        assertThat(result.attempts()).isEqualTo(1);
        assertThat(result.validation().summary().valid()).isTrue();
        // 빼지 않고 옮기기만 한다
        assertThat(result.draft().days()).flatExtracting(day -> day.visits()).extracting(v -> v.placeId())
                .containsExactlyInAnyOrder(DOTONBORI.placeId(), OsakaFixtures.CAFE_CLOSED_MONDAY.placeId());
        assertThat(result.draft().days().get(0).visits()).extracting(v -> v.placeId())
                .containsExactly(DOTONBORI.placeId());
        assertThat(result.excludedPlaceIds()).isEmpty();
    }

    @Test
    void 세_번_모두_걸리면_마지막_결과를_그대로_돌려준다() {
        givenUnrepairable(matched(DOTONBORI), matched(OsakaFixtures.CAFE_CLOSED_EVERY_DAY));
        String closedDay = """
                {"days": [{"dayNumber": 1, "placeNumbers": [1, 2]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """;
        llm.thenReturn(closedDay).thenReturn(closedDay).thenReturn(closedDay);

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(result.attempts()).isEqualTo(3);
        assertThat(result.validation().summary().valid()).isFalse();
        assertThat(result.draft().days().get(0).visits()).hasSize(2);
    }

    @Test
    void 옮겨도_안_되면_서버가_장소를_빼고_넣지_못한_장소로_알려_준다() {
        given(matched(DOTONBORI), matched(OsakaFixtures.CAFE_CLOSED_EVERY_DAY));
        // 여행 내내 휴무라 어디로 옮겨도 안 된다 → 서버가 빼고 끝낸다 (AI 재호출 없음)
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1, 2]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(llm.callCount()).isEqualTo(1);
        assertThat(result.validation().summary().valid()).isTrue();
        assertThat(result.excludedPlaceIds()).containsExactly(OsakaFixtures.CAFE_CLOSED_EVERY_DAY.placeId());
        assertThat(result.draft().days()).flatExtracting(day -> day.visits()).extracting(v -> v.placeId())
                .containsExactly(DOTONBORI.placeId());
    }

    @Test
    void 매칭된_장소가_없으면_400이다() {
        given(pending(DOTONBORI));

        assertThatThrownBy(() -> service.generate(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("장소가 없습니다");
        assertThat(llm.callCount()).isZero();
    }

    @Test
    void 만든_일정을_AI_생성으로_저장한다() {
        given(matched(DOTONBORI), matched(KUROMON));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": [2]},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);
        Mockito.when(itineraryService.saveAiDraft(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any()))
                .thenAnswer(call -> savedVersion(call.getArgument(2)));

        GeneratedItineraryVersion result = service.generateAndSave(1L, 7L);

        Mockito.verify(itineraryService).saveAiDraft(Mockito.eq(1L), Mockito.eq(7L), Mockito.any(), Mockito.any());
        assertThat(result.itinerary().version()).isEqualTo(1);
        assertThat(result.itinerary().draft().days()).hasSize(4);
    }

    @Test
    void 넣지_못한_장소는_저장_결과와_함께_돌려준다() {
        given(matched(DOTONBORI), matched(KUROMON), matched(UMEDA_SKY));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": [2]},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": [3]}
                """);
        Mockito.when(itineraryService.saveAiDraft(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any()))
                .thenAnswer(call -> savedVersion(call.getArgument(2)));

        GeneratedItineraryVersion result = service.generateAndSave(1L, 7L);

        assertThat(result.excludedPlaceIds()).containsExactly(UMEDA_SKY.placeId());
    }

    @Test
    void 생성에_실패하면_저장하지_않는다() {
        given(pending(DOTONBORI));

        assertThatThrownBy(() -> service.generateAndSave(1L, 7L)).isInstanceOf(BusinessException.class);
        Mockito.verifyNoInteractions(itineraryService);
    }

    @Test
    void 고른_장소만_AI에_넘긴다() {
        given(withId(1L, matched(DOTONBORI)), withId(2L, matched(KUROMON)), withId(3L, matched(OSAKA_CASTLE)));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": [2]},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        GeneratedItinerary result = service.generate(1L, 7L, List.of(3L, 1L));

        assertThat(result.draft().days()).flatExtracting(day -> day.visits()).extracting(v -> v.placeId())
                .containsExactlyInAnyOrder(DOTONBORI.placeId(), OSAKA_CASTLE.placeId());
        assertThat(llm.requests().get(0).toString()).doesNotContain(KUROMON.name());
    }

    @Test
    void 고른_장소_중_매칭된_장소만_쓴다() {
        given(withId(1L, matched(DOTONBORI)), withId(2L, pending(KUROMON)));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        GeneratedItinerary result = service.generate(1L, 7L, List.of(1L, 2L));

        assertThat(result.draft().days().get(0).visits()).extracting(v -> v.placeId())
                .containsExactly(DOTONBORI.placeId());
    }

    @Test
    void 고른_장소가_모두_매칭_전이면_400이다() {
        given(withId(1L, matched(DOTONBORI)), withId(2L, pending(KUROMON)));

        assertThatThrownBy(() -> service.generate(1L, 7L, List.of(2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("장소가 없습니다");
        assertThat(llm.callCount()).isZero();
    }

    @Test
    void 이_여행에_없는_장소_id가_섞이면_AI를_부르기_전에_400이다() {
        given(withId(1L, matched(DOTONBORI)), withId(2L, matched(KUROMON)));

        assertThatThrownBy(() -> service.generateAndSave(1L, 7L, List.of(1L, 99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이 여행에 없는 장소");
        assertThat(llm.callCount()).isZero();
        Mockito.verifyNoInteractions(itineraryService);
    }

    @Test
    void 영업시간을_알면_AI_프롬프트에_붙인다() {
        givenWithHours(Map.of(DOTONBORI.placeId(), OpeningHours.daily(LocalTime.of(9, 0), LocalTime.of(17, 0))),
                matched(DOTONBORI));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        service.generate(1L, 7L);

        assertThat(llm.requests().get(0).messages().toString()).contains("영업: 매일 09:00");
    }

    @Test
    void 여행_내내_영업하지_않는_장소는_AI에_넘기지_않고_따로_돌려준다() {
        givenWithHours(Map.of(KUROMON.placeId(), OpeningHours.weekly(List.of())),
                matched(DOTONBORI), matched(KUROMON));
        llm.thenReturn("""
                {"days": [{"dayNumber": 1, "placeNumbers": [1]},
                          {"dayNumber": 2, "placeNumbers": []},
                          {"dayNumber": 3, "placeNumbers": []},
                          {"dayNumber": 4, "placeNumbers": []}],
                 "excluded": []}
                """);

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(result.closedPlaceIds()).containsExactly(KUROMON.placeId());
        assertThat(result.excludedPlaceIds()).isEmpty();
        String prompt = llm.requests().get(0).messages().toString();
        assertThat(prompt).contains(DOTONBORI.name()).doesNotContain(KUROMON.name());
    }

    @Test
    void 장소가_모두_영업하지_않으면_AI를_부르지_않고_빈_일정을_돌려준다() {
        givenWithHours(Map.of(DOTONBORI.placeId(), OpeningHours.weekly(List.of()),
                        KUROMON.placeId(), OpeningHours.weekly(List.of())),
                matched(DOTONBORI), matched(KUROMON));

        GeneratedItinerary result = service.generate(1L, 7L);

        assertThat(llm.callCount()).isZero();
        assertThat(result.attempts()).isZero();
        assertThat(result.closedPlaceIds()).containsExactly(DOTONBORI.placeId(), KUROMON.placeId());
        assertThat(result.draft().days()).hasSize(4).allMatch(day -> day.visits().isEmpty());
    }

    // ---- helpers ----

    @Test
    void 장소가_일수에_비해_많으면_하루_쓸_수_있는_시간과_체류_시간을_알려_준다() {
        givenCrowded(21);   // 4일 × 하루 최대 5곳 = 20곳을 넘는다
        llm.thenReturn(allExcluded(21));

        service.generate(1L, 7L);

        // 시스템 프롬프트의 규칙 14에도 같은 말이 있어서 사용자 프롬프트(두 번째 메시지)만 본다
        String prompt = llm.requests().get(0).messages().get(1).content();
        assertThat(prompt).contains("하루 쓸 수 있는 시간: 09:30~21:30 (720분)").contains("체류 90분");
    }

    @Test
    void 장소가_많지_않으면_하루_시간을_알리지_않는다() {
        givenCrowded(20);
        llm.thenReturn(allExcluded(20));

        service.generate(1L, 7L);

        String prompt = llm.requests().get(0).messages().get(1).content();
        assertThat(prompt).doesNotContain("하루 쓸 수 있는 시간").doesNotContain("체류 90분");
    }

    private static String allExcluded(int count) {
        String numbers = java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        return "{\"days\": [{\"dayNumber\": 1, \"placeNumbers\": []}, {\"dayNumber\": 2, \"placeNumbers\": []},"
                + " {\"dayNumber\": 3, \"placeNumbers\": []}, {\"dayNumber\": 4, \"placeNumbers\": []}],"
                + " \"excluded\": [" + numbers + "]}";
    }

    /** 서로 다른 place ID를 가진 매칭된 장소를 count개 만든다. 모두 관광지라 체류 90분. */
    private void givenCrowded(int count) {
        SavedPlace[] places = new SavedPlace[count];
        for (int i = 0; i < count; i++) {
            SavedPlace saved = Mockito.mock(SavedPlace.class);
            Mockito.when(saved.getMatchStatus()).thenReturn(PlaceMatchStatus.MATCHED);
            Mockito.when(saved.getPlaceId()).thenReturn("x" + i);
            Mockito.when(saved.getName()).thenReturn("장소" + i);
            Mockito.when(saved.getCategory()).thenReturn(com.dotlo.line4thon_18team.place.PlaceCategory.SIGHTSEEING);
            places[i] = saved;
        }
        given(places);
    }

    /** 서버가 옮기거나 빼도 나아지지 않는 상황. AI에게 다시 배치를 맡기는 길을 보기 위한 것 */
    private void givenUnrepairable(SavedPlace... places) {
        ValidationResult noBetter = ValidationResult.of(List.of(), List.of(
                new ValidationIssue(IssueCode.CLOSED_ON_DAY, Severity.ERROR, IssueKind.SCHEDULE_CONFLICT,
                        OsakaFixtures.DAY1, null, null, Map.of())));
        ItineraryValidator validator = new ItineraryValidator(OsakaFixtures.places(),
                FakeTravelTimeProvider.fixedMinutes(15), ValidationPolicy.defaults());
        ValidationRun run = new ValidationRun(trip(), List.of(places), validator, OsakaFixtures.trip(),
                PreferenceProfile.defaults(), placeId -> null, placeIds -> { }, ValidationPolicy.defaults(),
                draft -> noBetter);
        Mockito.when(validationService.prepare(1L, 7L)).thenReturn(run);
    }

    /** 검증은 실제 오사카 장소 정보로, AI에게 알려 줄 영업시간은 hours로 따로 준다. */
    private void givenWithHours(Map<String, OpeningHours> hours, SavedPlace... places) {
        FakePlaceInfoProvider provider = OsakaFixtures.places();
        ItineraryValidator validator = new ItineraryValidator(provider,
                FakeTravelTimeProvider.fixedMinutes(15), ValidationPolicy.defaults());
        ValidationRun run = new ValidationRun(trip(), List.of(places), validator,
                OsakaFixtures.trip(), PreferenceProfile.defaults(), hours::get);
        Mockito.when(validationService.prepare(1L, 7L)).thenReturn(run);
    }

    private static SavedPlace withId(long id, SavedPlace place) {
        Mockito.when(place.getId()).thenReturn(id);
        return place;
    }

    private static ItineraryVersionView savedVersion(Object draft) {
        return new ItineraryVersionView(1, ItinerarySource.AI_GENERATED, null, 7L, LocalDateTime.now(),
                (com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft) draft, "{}");
    }

    private void given(SavedPlace... places) {
        FakePlaceInfoProvider provider = OsakaFixtures.places();
        ItineraryValidator validator = new ItineraryValidator(provider,
                FakeTravelTimeProvider.fixedMinutes(15), ValidationPolicy.defaults());
        ValidationRun run = new ValidationRun(trip(), List.of(places), validator,
                OsakaFixtures.trip(), PreferenceProfile.defaults());
        Mockito.when(validationService.prepare(1L, 7L)).thenReturn(run);
    }

    private static Trip trip() {
        User user = User.builder().googleSub("sub").email("a@b.com").name("주인").build();
        return Trip.create(user, "오사카", OsakaFixtures.DAY1, OsakaFixtures.DAY4, Companion.ALONE, null);
    }

    private static SavedPlace matched(PlaceInfo place) {
        return savedPlace(place, PlaceMatchStatus.MATCHED, place.placeId());
    }

    private static SavedPlace pending(PlaceInfo place) {
        return savedPlace(place, PlaceMatchStatus.PENDING, null);
    }

    // SavedPlace 는 Takeout 용 생성 메서드만 있고 매칭 결과를 채울 방법이 없어서, 읽는 값만 가짜로 만든다
    private static SavedPlace savedPlace(PlaceInfo place, PlaceMatchStatus status, String placeId) {
        SavedPlace saved = Mockito.mock(SavedPlace.class);
        Mockito.when(saved.getMatchStatus()).thenReturn(status);
        Mockito.when(saved.getPlaceId()).thenReturn(placeId);
        Mockito.when(saved.getName()).thenReturn(place.name());
        Mockito.when(saved.getCategory()).thenReturn(place.category());
        return saved;
    }
}
