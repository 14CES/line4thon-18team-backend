package com.dotlo.line4thon_18team.preference.domain;

import com.dotlo.line4thon_18team.place.PlaceCategory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 취향 값 규칙. 저장 API와 LLM 해석 결과 검사가 같이 쓴다. */
public final class PreferenceRules {

    public static final int MAX_RAW_TEXT_LENGTH = 500;
    public static final int MAX_NOTES = 5;
    public static final int MAX_NOTE_LENGTH = 100;

    private PreferenceRules() {
    }

    /** 저장하기 전에 확인할 규칙 위반 목록. 비어 있으면 통과. */
    public static List<String> violations(TravelPace pace, WalkingPreference walking, DayStartPreference dayStart,
                                          List<PlaceCategory> interests, List<PlaceCategory> avoid, List<String> notes) {
        List<String> violations = new ArrayList<>();
        if (pace == null) {
            violations.add("pace는 필수입니다");
        }
        if (walking == null) {
            violations.add("walking은 필수입니다");
        }
        if (dayStart == null) {
            violations.add("dayStart는 필수입니다");
        }
        checkCategories("interests", interests, violations);
        checkCategories("avoid", avoid, violations);

        Set<PlaceCategory> overlap = new LinkedHashSet<>(nonNull(interests));
        overlap.retainAll(new LinkedHashSet<>(nonNull(avoid)));
        if (!overlap.isEmpty()) {
            violations.add("같은 유형을 interests와 avoid에 동시에 넣을 수 없습니다: " + overlap);
        }

        if (notes != null) {
            if (notes.stream().anyMatch(Objects::isNull)) {
                violations.add("notes에 빈 값(null)이 있습니다");
            }
            List<String> cleaned = cleanNotes(notes);
            if (cleaned.size() > MAX_NOTES) {
                violations.add("notes는 최대 " + MAX_NOTES + "개입니다 (받은 개수: " + cleaned.size() + ")");
            }
            for (String note : cleaned) {
                if (note.length() > MAX_NOTE_LENGTH) {
                    violations.add("notes 항목은 " + MAX_NOTE_LENGTH + "자 이하여야 합니다: " + note.substring(0, 20) + "...");
                }
            }
        }
        return violations;
    }

    /** 중복을 빼고 순서는 유지한다. */
    public static <T> List<T> distinct(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(nonNull(values)));
    }

    /** 앞뒤 공백 제거, 빈 문자열·중복 제거. */
    public static List<String> cleanNotes(List<String> notes) {
        if (notes == null) {
            return List.of();
        }
        return notes.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(note -> !note.isEmpty())
                .distinct()
                .toList();
    }

    private static void checkCategories(String field, List<PlaceCategory> categories, List<String> violations) {
        if (categories == null) {
            return;
        }
        if (categories.stream().anyMatch(Objects::isNull)) {
            violations.add(field + "에 빈 값(null)이 있습니다");
        }
        if (categories.stream().filter(Objects::nonNull).anyMatch(c -> !c.selectableAsPreference())) {
            violations.add(field + "에는 OTHER를 넣을 수 없습니다");
        }
    }

    private static <T> List<T> nonNull(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).toList();
    }
}
