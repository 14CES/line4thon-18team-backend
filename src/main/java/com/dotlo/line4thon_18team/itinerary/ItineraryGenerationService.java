package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.itinerary.ItineraryValidationService.ValidationRun;
import com.dotlo.line4thon_18team.itinerary.ai.HoursText;
import com.dotlo.line4thon_18team.itinerary.ai.PlanGenerationInput;
import com.dotlo.line4thon_18team.itinerary.ai.PlanGenerationOutput;
import com.dotlo.line4thon_18team.itinerary.ai.PlanGenerationTask;
import com.dotlo.line4thon_18team.itinerary.validation.model.DayDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.OpeningHours;
import com.dotlo.line4thon_18team.itinerary.validation.model.ValidationOrigin;
import com.dotlo.line4thon_18team.itinerary.validation.model.VisitDraft;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 일정 생성 (GEN-PLAN-001).
 *
 * <pre>
 *   저장 장소 → [AI가 일차별로 배치] → [검증기가 시각 계산] → 문제 없으면 끝
 *                      ↑                        ↓ 문제 있으면
 *                      └── 문제를 알려주고 다시 배치 (최대 2회)
 * </pre>
 *
 * <p>AI는 "어떤 장소를 며칠째 몇 번째로" 까지만 정하고, 시각은 검증기가 이동시간·영업시간으로 계산한다.
 */
@Service
@RequiredArgsConstructor

public class ItineraryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGenerationService.class);

    /** 처음 배치 1회 + 검증에 걸렸을 때 다시 배치 2회 (명세서: 내부에서 검증/최대 2회 재생성) */
    private static final int MAX_ATTEMPTS = 3;
    // 장소 사이 이동시간의 어림값(분). 검증이 실제 경로로 계산하는 값이 아니라 AI가 하루 분량을 가늠하는 용도다
    private static final int AVERAGE_TRAVEL_MINUTES = 25;

    /**
     * 서버가 일정을 고치는 횟수. 한 번 고칠 때마다 실제 검증을 한 번 더 한다(약 3초). AI에게 다시
     * 배치해 달라고 하는 데 20~45초가 드니, 몇 번 더 고쳐 보는 것이 싸다.
     */
    private static final int MAX_REPAIR_ROUNDS = 3;

    private final ItineraryValidationService validationService;
    private final StructuredLlmExecutor llm;
    private final PlanGenerationTask planGenerationTask;
    private final ItineraryService itineraryService;   // BE1 일정 저장

    /**
     * 일정을 만들어 새 버전으로 저장한다 (GEN-PLAN-001). API에서 부르는 진입점이다.
     *
     * <p>저장은 AI 응답을 다 받은 뒤 한 번만 일어난다. 중간에 실패하면 아무것도 저장되지 않는다.
     *
     * @throws BusinessException 남의 여행·없는 여행(404), 배치할 장소가 없음(400), 숙소를 일부만 넣음(400)
     */
    public GeneratedItineraryVersion generateAndSave(long tripId, long userId) {
        return generateAndSave(tripId, userId, null);
    }

    /**
     * 고른 장소로 일정을 만들어 새 버전으로 저장한다.
     *
     * @param savedPlaceIds 고른 저장 장소 id. null이면 저장 장소 전체
     * @throws BusinessException 이 여행에 없는 장소 id가 섞임(400, AI 호출 전)
     */
    public GeneratedItineraryVersion generateAndSave(long tripId, long userId, Collection<Long> savedPlaceIds) {
        GeneratedItinerary generated = generate(tripId, userId, savedPlaceIds);
        ItineraryVersionView saved = itineraryService.saveAiDraft(tripId, userId,
                generated.draft(), generated.validation());
        return new GeneratedItineraryVersion(saved, generated.excludedPlaceIds(), generated.closedPlaceIds());
    }

    /**
     * 일정을 만들기만 하고 저장하지 않는다. 저장까지 하려면 {@link #generateAndSave}를 쓴다.
     *
     * <p>이 메서드에는 트랜잭션을 걸지 않는다. AI 응답을 기다리는 동안 DB 연결을 쥐고 있지 않기 위해서다.
     * 조회는 {@link ItineraryValidationService#prepare}가 자기 트랜잭션에서 하고, 돌려받은 엔티티는
     * 기본 필드만 읽는다 (연관 관계를 더 타면 트랜잭션이 이미 닫혀 있다).
     * @throws BusinessException 남의 여행·없는 여행(404), 배치할 장소가 없음(400)
     */
    public GeneratedItinerary generate(long tripId, long userId) {
        return generate(tripId, userId, null);
    }

    /** @param savedPlaceIds 고른 저장 장소 id. null이면 저장 장소 전체 */
    public GeneratedItinerary generate(long tripId, long userId, Collection<Long> savedPlaceIds) {
        ValidationRun run = validationService.prepare(tripId, userId);
        long hoursStart = System.nanoTime();
        Candidates candidates = buildCandidates(run, selected(run.places(), savedPlaceIds));
        log.info("영업시간 조회 tripId={} places={} elapsed={}ms", tripId,
                candidates.open().size() + candidates.closedPlaceIds().size(), (System.nanoTime() - hoursStart) / 1_000_000);
        if (candidates.open().isEmpty()) {
            // 모든 장소가 여행 내내 영업하지 않는다. AI에게 배치할 것이 없어서 부르지 않는다
            return emptyItinerary(run, candidates.closedPlaceIds());
        }
        Trip trip = run.trip();
        PlanGenerationInput input = PlanGenerationInput.of(trip.getCity(), dayCount(trip), trip.getCompanion(),
                run.preferences(), candidates.open(),
                capacity(run, candidates.open().size(), dayCount(trip)));

        GeneratedItinerary last = null;
        List<String> problems = List.of();
        long startedAt = System.nanoTime();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long llmStart = System.nanoTime();
            PlanGenerationOutput plan = llm.execute(planGenerationTask, input.retryWith(problems)).value();
            long llmEnd = System.nanoTime();
            ItineraryDraft draft = toDraft(plan, input, run.trip().getStartDate());
            ValidationResult validation = run.validate(draft, ValidationOrigin.AI_GENERATED);
            long validateEnd = System.nanoTime();
            log.info("일정 생성 시간 tripId={} attempt={}/{} llm={}ms validate={}ms total={}ms valid={}",
                    tripId, attempt, MAX_ATTEMPTS, (llmEnd - llmStart) / 1_000_000,
                    (validateEnd - llmEnd) / 1_000_000, (validateEnd - startedAt) / 1_000_000,
                    validation.summary().valid());

            List<String> excluded = placeIds(plan.excluded(), input);
            if (!validation.summary().valid()) {
                // AI는 도착 시각을 계산할 수 없어서 영업시간 문제를 스스로 못 고친다. 서버가 방문을 옮기거나
                // 빼서 풀리는지 먼저 보고, 풀리면 AI를 다시 부르지 않는다 (재배치는 20~45초가 더 들고,
                // 다시 배치한 결과가 더 나쁠 때도 있다)
                long repairStart = System.nanoTime();
                int errorsBefore = validation.summary().errorCount();
                List<String> dropped = new ArrayList<>();
                int rounds = 0;
                /*
                 * 여러 번 나눠 고친다. 고치는 동안에는 이미 조회한 이동시간만 쓰고 새로 생긴 구간은
                 * 직선거리로 추정하는데, 그 추정이 실제와 크게 어긋날 때가 있다. 한 번 고친 일정을 실제로
                 * 검증하면 새 구간의 실제 이동시간이 쌓이므로, 다음 번에는 더 정확하게 고칠 수 있다.
                 */
                for (rounds = 1; rounds <= MAX_REPAIR_ROUNDS; rounds++) {
                    ItineraryRepairer.Repair repair = ItineraryRepairer.repair(draft, run.repairCheck(),
                            ItineraryRepairer.MAX_DROPS - dropped.size());
                    if (repair.draft() == draft) {
                        break;   // 옮기거나 빼도 나아지지 않는다
                    }
                    ValidationResult repaired = run.validate(repair.draft(), ValidationOrigin.AI_GENERATED);
                    if (repaired.summary().errorCount() >= validation.summary().errorCount()) {
                        break;   // 추정으로는 나아졌지만 실제로는 아니다. 고친 것을 버린다
                    }
                    draft = repair.draft();
                    validation = repaired;
                    dropped.addAll(repair.droppedPlaceIds());
                    if (validation.summary().valid()) {
                        break;
                    }
                }
                if (validation.summary().errorCount() < errorsBefore) {
                    excluded = alsoExcluded(excluded, dropped);
                    log.info("일정 고침 tripId={} attempt={}/{} errors={}->{} dropped={} rounds={} valid={} elapsed={}ms",
                            tripId, attempt, MAX_ATTEMPTS, errorsBefore, validation.summary().errorCount(),
                            dropped.size(), rounds, validation.summary().valid(),
                            (System.nanoTime() - repairStart) / 1_000_000);
                }
            }

            last = new GeneratedItinerary(draft, validation, excluded, candidates.closedPlaceIds(), attempt);
            if (validation.summary().valid()) {
                return last;
            }
            problems = describe(validation, draft, input);
            log.info("일정 재생성 tripId={} attempt={}/{} problems={}", tripId, attempt, MAX_ATTEMPTS, problems);
        }
        // 끝까지 문제가 남으면 마지막 결과를 그대로 돌려준다. 화면에서 경고로 보여주고 사용자가 고치면 된다.
        return last;
    }

    /** 고른 장소만 남긴다 (null이면 전체). 이 여행에 없는 id가 섞이면 AI를 부르기 전에 400 */
    private static List<SavedPlace> selected(List<SavedPlace> places, Collection<Long> savedPlaceIds) {
        if (savedPlaceIds == null) {
            return places;
        }
        Set<Long> ids = new HashSet<>(savedPlaceIds);
        List<SavedPlace> picked = places.stream().filter(place -> ids.contains(place.getId())).toList();
        if (picked.size() != ids.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이 여행에 없는 장소가 있습니다.");
        }
        return picked;
    }

    /** AI에게 넘길 장소(영업시간 포함)와, 여행 내내 영업하지 않아 넘기지 않고 뺀 장소. */
    private record Candidates(List<PlanGenerationInput.PlaceRef> open, List<String> closedPlaceIds) {
    }

    /**
     * 매칭이 끝난 장소만 쓴다. place ID가 없으면 영업시간·이동시간을 조회할 수 없다.
     * 여행 기간 내내 영업하지 않는 장소는 AI에게 넘기지 않고 따로 돌려준다 (영업시간을 모르는 장소는 넘긴다).
     */
    private static Candidates buildCandidates(ValidationRun run, List<SavedPlace> places) {
        List<SavedPlace> usable = places.stream()
                .filter(place -> place.getMatchStatus() == PlaceMatchStatus.MATCHED && place.getPlaceId() != null)
                .toList();
        if (usable.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_PLACES_TO_PLAN);
        }
        run.prefetchHours().accept(usable.stream().map(SavedPlace::getPlaceId).toList());   // 장소가 많아도 조회를 동시에
        Trip trip = run.trip();
        int dayCount = dayCount(trip);
        List<PlanGenerationInput.PlaceRef> open = new ArrayList<>();
        List<String> closed = new ArrayList<>();
        for (SavedPlace place : usable) {
            OpeningHours hours = run.hoursOf().apply(place.getPlaceId());
            if (HoursText.closedEveryDay(hours, trip.getStartDate(), dayCount)) {
                if (!closed.contains(place.getPlaceId())) {
                    closed.add(place.getPlaceId());
                }
                continue;
            }
            PlaceCategory category = place.getCategory() == null ? PlaceCategory.OTHER : place.getCategory();
            open.add(new PlanGenerationInput.PlaceRef(place.getPlaceId(), place.getName(), category,
                    HoursText.forTrip(hours, trip.getStartDate(), dayCount),
                    run.policy().stayMinutes(category, run.preferences().pace())));
        }
        return new Candidates(open, closed);
    }

    /**
     * AI에게 알려 줄 하루 분량. 검증이 쓰는 하루 시작·끝과 방문 사이 여유를 같이 쓴다.
     * 장소가 하루 개수 기준으로도 넘칠 만큼 많을 때만 알린다. 작은 여행은 AI가 따질 것만 늘어 응답이 느려진다
     * (장소 8곳 3일에서 AI 응답이 평균 약 9초 → 약 16초).
     */
    private static PlanGenerationInput.DayCapacity capacity(ValidationRun run, int placeCount, int dayCount) {
        PreferenceProfile preferences = run.preferences();
        if (placeCount <= dayCount * PlanGenerationTask.maxPlacesPerDay(preferences.pace())) {
            return null;
        }
        return new PlanGenerationInput.DayCapacity(run.policy().dayStart(preferences.dayStart()), run.policy().dayEnd(),
                run.policy().bufferMinutes(preferences.pace()) + AVERAGE_TRAVEL_MINUTES);
    }

    /** 방문이 하나도 없는 일정. 모든 날짜를 빈 날로 채운다. */
    private static GeneratedItinerary emptyItinerary(ValidationRun run, List<String> closedPlaceIds) {
        Trip trip = run.trip();
        List<DayDraft> days = new ArrayList<>();
        for (int i = 0; i < dayCount(trip); i++) {
            days.add(new DayDraft(trip.getStartDate().plusDays(i), List.of()));
        }
        ItineraryDraft draft = new ItineraryDraft(days);
        ValidationResult validation = run.validate(draft, ValidationOrigin.AI_GENERATED);
        return new GeneratedItinerary(draft, validation, List.of(), closedPlaceIds, 0);
    }

    private static int dayCount(Trip trip) {
        return (int) ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
    }

    /** 장소 번호를 place ID로 되돌리고 itemId를 새로 만든다. */
    private static ItineraryDraft toDraft(PlanGenerationOutput plan, PlanGenerationInput input, LocalDate startDate) {
        List<DayDraft> days = new ArrayList<>();
        for (PlanGenerationOutput.Day day : plan.days()) {
            LocalDate date = startDate.plusDays(day.dayNumber() - 1L);
            List<VisitDraft> visits = day.placeNumbers().stream()
                    .map(number -> VisitDraft.of(ItineraryItemId.newId(), input.byNumber(number).placeId()))
                    .toList();
            days.add(new DayDraft(date, visits));
        }
        return new ItineraryDraft(days);
    }

    private static List<String> placeIds(List<Integer> numbers, PlanGenerationInput input) {
        return numbers.stream().map(number -> input.byNumber(number).placeId()).toList();
    }

    /** AI가 빼둔 장소에 서버가 뺀 장소를 더한다. 화면에서는 둘 다 '일정에 넣지 못한 장소'로 같게 보인다. */
    private static List<String> alsoExcluded(List<String> excluded, List<String> dropped) {
        List<String> all = new ArrayList<>(excluded);
        dropped.stream().filter(placeId -> !all.contains(placeId)).forEach(all::add);
        return List.copyOf(all);
    }

    private static List<String> describe(ValidationResult validation, ItineraryDraft draft,
                                         PlanGenerationInput input) {
        Map<String, String> nameByPlaceId = new LinkedHashMap<>();
        input.places().forEach(place -> nameByPlaceId.put(place.placeId(), place.name()));
        return ItineraryProblems.describe(validation, draft, nameByPlaceId);
    }
}
