package com.dotlo.line4thon_18team.preference.ai;

import java.util.List;

/**
 * 취향 해석 LLM 출력. 필드는 preference-interpretation.schema.json과 같다.
 * 값은 스키마 검사로 보장되므로 문자열로 받고, 도메인 enum으로 바꾸는 건 {@link PreferenceInterpretationTask}가 한다.
 */
public record PreferenceInterpretationOutput(String pace,
                                             String walking,
                                             String dayStart,
                                             List<String> interests,
                                             List<String> avoid,
                                             List<String> notes) {

    /** 사용자가 언급하지 않은 항목. */
    public static final String UNSPECIFIED = "UNSPECIFIED";

    public PreferenceInterpretationOutput {
        interests = interests == null ? List.of() : List.copyOf(interests);
        avoid = avoid == null ? List.of() : List.copyOf(avoid);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
