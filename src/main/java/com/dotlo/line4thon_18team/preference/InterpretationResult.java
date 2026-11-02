package com.dotlo.line4thon_18team.preference;

import com.dotlo.line4thon_18team.preference.domain.ChipType;
import com.dotlo.line4thon_18team.preference.domain.PreferenceChip;
import com.dotlo.line4thon_18team.preference.domain.PreferenceChips;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;

import java.util.List;
import java.util.Set;

/**
 * 취향 해석 응답 (저장 전). 사용자가 칩을 확인·수정한 뒤 PUT으로 저장한다.
 *
 * @param status         해석 상태
 * @param preferences    기본값까지 채운 전체 취향 (그대로 저장 요청에 쓸 수 있음)
 * @param chips          AI가 이해한 내용만 담은 칩 (언급 안 한 항목은 빠짐)
 * @param fallbackReason status가 FALLBACK_DEFAULT일 때 이유
 */
public record InterpretationResult(InterpretationStatus status,
                                   PreferenceProfile preferences,
                                   List<PreferenceChip> chips,
                                   FallbackReason fallbackReason) {

    public InterpretationResult {
        chips = chips == null ? List.of() : List.copyOf(chips);
    }

    static InterpretationResult interpreted(PreferenceProfile profile, Set<ChipType> explicitSingles) {
        return new InterpretationResult(InterpretationStatus.INTERPRETED, profile,
                PreferenceChips.of(profile, explicitSingles), null);
    }

    static InterpretationResult emptyInput() {
        return new InterpretationResult(InterpretationStatus.EMPTY_INPUT, PreferenceProfile.defaults(), List.of(), null);
    }

    static InterpretationResult fallback(FallbackReason reason) {
        return new InterpretationResult(InterpretationStatus.FALLBACK_DEFAULT, PreferenceProfile.defaults(), List.of(), reason);
    }
}
