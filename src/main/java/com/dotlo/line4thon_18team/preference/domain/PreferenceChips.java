package com.dotlo.line4thon_18team.preference.domain;

import com.dotlo.line4thon_18team.place.PlaceCategory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 취향 프로필을 화면용 칩 목록으로 바꾼다. */
public final class PreferenceChips {

    private static final Set<ChipType> ALL_SINGLES = EnumSet.of(ChipType.PACE, ChipType.WALKING, ChipType.DAY_START);

    private PreferenceChips() {
    }

    /** 저장된 취향 전체를 칩으로 (기본값 항목 포함). */
    public static List<PreferenceChip> all(PreferenceProfile profile) {
        return of(profile, ALL_SINGLES);
    }

    /**
     * @param explicitSingles 강도·걷기·시작시각 중 칩으로 보여줄 항목.
     *                        AI 해석 결과에서는 사용자가 실제로 언급한 항목만 넣는다.
     */
    public static List<PreferenceChip> of(PreferenceProfile profile, Set<ChipType> explicitSingles) {
        List<PreferenceChip> chips = new ArrayList<>();
        if (explicitSingles.contains(ChipType.PACE)) {
            chips.add(new PreferenceChip(ChipType.PACE, profile.pace().name(), profile.pace().label()));
        }
        if (explicitSingles.contains(ChipType.WALKING)) {
            chips.add(new PreferenceChip(ChipType.WALKING, profile.walking().name(), profile.walking().label()));
        }
        if (explicitSingles.contains(ChipType.DAY_START)) {
            chips.add(new PreferenceChip(ChipType.DAY_START, profile.dayStart().name(), profile.dayStart().label()));
        }
        for (PlaceCategory category : profile.interests()) {
            chips.add(new PreferenceChip(ChipType.INTEREST, category.name(), category.label()));
        }
        for (PlaceCategory category : profile.avoid()) {
            chips.add(new PreferenceChip(ChipType.AVOID, category.name(), category.label() + " 빼기"));
        }
        for (String note : profile.notes()) {
            chips.add(new PreferenceChip(ChipType.NOTE, note, note));
        }
        return List.copyOf(chips);
    }
}
