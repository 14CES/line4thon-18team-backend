package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.model.DayDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.VisitDraft;
import com.dotlo.line4thon_18team.itinerary.validation.result.Severity;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationIssue;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 검증에 걸린 일정을 서버가 먼저 고쳐 본다. 방문을 옮겨 보고, 옮겨서 풀리지 않을 때만 장소를 뺀다.
 *
 * <p>AI는 장소에 몇 시에 도착하는지 계산할 수 없어서 "도착 17:11인데 영업은 17:00까지" 같은 문제를
 * 스스로 고치지 못한다. 시각을 계산하는 쪽은 서버라서, 같은 날 앞쪽으로 당기거나 다른 날로 옮기면
 * 풀리는 경우가 많다. AI에게 다시 배치해 달라고 하면 20~45초가 더 들고, 다시 배치한 결과가 더 나쁠 때도 있다.
 *
 * <p>한 번에 방문 하나씩 옮겨 보고 오류가 가장 많이 줄어드는 이동을 고른다. 어디로 옮겨도 나아지지 않으면
 * 그때만 장소를 뺀다 (최대 {@link #MAX_DROPS}곳). 뺀 장소는 '일정에 넣지 못한 장소'로 돌려주므로
 * 화면에서는 AI가 빼는 장소와 같게 보인다. 둘 다 안 되면 받은 일정을 그대로 돌려줘서, 고치려다 더
 * 나빠지는 일은 없다.
 */
final class ItineraryRepairer {

    /** 방문을 옮기거나 빼는 횟수의 상한. 한 번마다 전체를 다시 평가한다 */
    static final int MAX_ROUNDS = 6;
    /** 뺄 수 있는 장소 수의 상한. 넘치는 일정을 장소를 다 빼서 통과시키지 않으려는 안전장치 */
    static final int MAX_DROPS = 3;
    /** 평가 횟수의 상한. 장소가 많은 여행에서 후보가 너무 늘어나지 않게 막는다 */
    static final int MAX_CHECKS = 600;

    private ItineraryRepairer() {
    }

    /**
     * @param draft            고칠 일정
     * @param droppedPlaceIds  옮겨도 풀리지 않아 일정에서 뺀 장소. 없으면 빈 목록
     */
    record Repair(ItineraryDraft draft, List<String> droppedPlaceIds) {

        Repair {
            droppedPlaceIds = droppedPlaceIds == null ? List.of() : List.copyOf(droppedPlaceIds);
        }
    }

    /**
     * @param check 고쳐 본 일정을 평가하는 함수. 이미 조회한 이동시간만 쓰고 구글을 새로 부르지 않는
     *              가벼운 검증이어야 한다 (후보가 수백 개라 조회하면 느려진다)
     * @return 오류가 줄어든 일정. 더 나아지지 않으면 받은 일정 그대로 (같은 객체)
     */
    static Repair repair(ItineraryDraft draft, Function<ItineraryDraft, ValidationResult> check) {
        return repair(draft, check, MAX_DROPS);
    }

    /**
     * @param dropBudget 이번에 뺄 수 있는 장소 수. 여러 번 나눠 고칠 때 남은 몫을 넘긴다
     */
    static Repair repair(ItineraryDraft draft, Function<ItineraryDraft, ValidationResult> check, int dropBudget) {
        int[] checks = {1};
        ItineraryDraft best = draft;
        ValidationResult bestResult = check.apply(best);
        int bestErrors = errors(bestResult);
        List<String> dropped = new ArrayList<>();

        for (int round = 0; round < MAX_ROUNDS && bestErrors > 0 && checks[0] < MAX_CHECKS; round++) {
            Set<String> targets = problemItems(best, bestResult);
            Change change = bestChange(best, targets, bestErrors, checks, check, false);
            if (change == null && dropped.size() < dropBudget) {
                // 어디로 옮겨도 나아지지 않는다. 이제 장소를 빼 본다
                change = bestChange(best, targets, bestErrors, checks, check, true);
                if (change != null) {
                    dropped.add(change.droppedPlaceId());
                }
            }
            if (change == null) {
                break;
            }
            best = change.draft();
            bestResult = change.result();
            bestErrors = change.errors();
        }
        return new Repair(best, dropped);
    }

    /** 가장 좋아지는 후보 하나. 더 좋아지는 후보가 없으면 null */
    private static Change bestChange(ItineraryDraft draft, Set<String> targets, int currentErrors, int[] checks,
                                     Function<ItineraryDraft, ValidationResult> check, boolean drop) {
        Change found = null;
        int foundErrors = currentErrors;
        for (String itemId : targets) {
            for (Candidate candidate : drop ? drops(draft, itemId) : moves(draft, itemId)) {
                if (checks[0] >= MAX_CHECKS) {
                    return found;
                }
                ValidationResult result = check.apply(candidate.draft());
                checks[0]++;
                int errors = errors(result);
                if (errors < foundErrors) {
                    found = new Change(candidate.draft(), result, errors, candidate.droppedPlaceId());
                    foundErrors = errors;
                }
            }
        }
        return found;
    }

    static int errors(ValidationResult validation) {
        return validation.summary().errorCount();
    }

    private record Candidate(ItineraryDraft draft, String droppedPlaceId) {
    }

    private record Change(ItineraryDraft draft, ValidationResult result, int errors, String droppedPlaceId) {
    }

    /**
     * 옮기거나 뺄 방문. 오류가 걸린 방문과, 하루가 넘친 날의 모든 방문이다
     * (하루 초과는 날 단위 문제라서 어느 방문을 옮겨도 풀릴 수 있다).
     */
    private static Set<String> problemItems(ItineraryDraft draft, ValidationResult validation) {
        Set<String> items = new LinkedHashSet<>();
        Set<LocalDate> crowdedDays = new LinkedHashSet<>();
        for (ValidationIssue issue : validation.issues()) {
            if (issue.severity() != Severity.ERROR) {
                continue;
            }
            if (issue.itemId() != null) {
                items.add(issue.itemId());
            } else {
                crowdedDays.add(issue.date());
            }
        }
        for (DayDraft day : draft.days()) {
            if (crowdedDays.contains(day.date())) {
                day.visits().forEach(visit -> items.add(visit.itemId()));
            }
        }
        return items;
    }

    /** 이 방문을 하루 안의 다른 자리와 다른 날의 모든 자리에 넣어 본 일정들. 원래 자리는 뺀다. */
    private static List<Candidate> moves(ItineraryDraft draft, String itemId) {
        Spot spot = find(draft, itemId);
        if (spot == null) {
            return List.of();
        }
        List<List<VisitDraft>> without = without(draft, spot);
        List<Candidate> candidates = new ArrayList<>();
        for (int d = 0; d < without.size(); d++) {
            for (int at = 0; at <= without.get(d).size(); at++) {
                if (d == spot.day() && at == spot.index()) {
                    continue;   // 원래 자리
                }
                candidates.add(new Candidate(rebuild(draft, without, spot.visit(), d, at), null));
            }
        }
        return candidates;
    }

    /** 이 방문을 일정에서 뺀 일정 하나. */
    private static List<Candidate> drops(ItineraryDraft draft, String itemId) {
        Spot spot = find(draft, itemId);
        if (spot == null) {
            return List.of();
        }
        return List.of(new Candidate(rebuild(draft, without(draft, spot), spot.visit(), -1, -1),
                spot.visit().placeId()));
    }

    private record Spot(int day, int index, VisitDraft visit) {
    }

    private static Spot find(ItineraryDraft draft, String itemId) {
        for (int d = 0; d < draft.days().size(); d++) {
            List<VisitDraft> visits = draft.days().get(d).visits();
            for (int v = 0; v < visits.size(); v++) {
                if (visits.get(v).itemId().equals(itemId)) {
                    return new Spot(d, v, visits.get(v));
                }
            }
        }
        return null;
    }

    /** 그 방문을 뺀 날짜별 방문 목록. */
    private static List<List<VisitDraft>> without(ItineraryDraft draft, Spot spot) {
        List<List<VisitDraft>> days = new ArrayList<>();
        for (int d = 0; d < draft.days().size(); d++) {
            List<VisitDraft> visits = new ArrayList<>(draft.days().get(d).visits());
            if (d == spot.day()) {
                visits.remove(spot.index());
            }
            days.add(visits);
        }
        return days;
    }

    /** toDay가 -1이면 다시 넣지 않는다 (빼기). */
    private static ItineraryDraft rebuild(ItineraryDraft draft, List<List<VisitDraft>> without, VisitDraft moving,
                                          int toDay, int at) {
        List<DayDraft> days = new ArrayList<>();
        for (int d = 0; d < without.size(); d++) {
            List<VisitDraft> visits = new ArrayList<>(without.get(d));
            if (d == toDay) {
                visits.add(at, moving);
            }
            days.add(new DayDraft(draft.days().get(d).date(), visits));
        }
        return new ItineraryDraft(days);
    }
}
