package com.dotlo.line4thon_18team.preference.domain;

import com.dotlo.line4thon_18team.place.PlaceCategory;

import java.util.List;
import java.util.Objects;

/**
 * 확정된 취향 (칩 스키마). 일정 생성·검증의 입력이 된다.
 *
 * @param pace      일정 강도
 * @param walking   걷기 선호
 * @param dayStart  하루 시작 시각 선호
 * @param interests 가고 싶은 장소 유형
 * @param avoid     빼고 싶은 장소 유형
 * @param notes     칩으로 표현할 수 없는 조건 (예: "해산물 알레르기가 있어요")
 */
public record PreferenceProfile(TravelPace pace,
                                WalkingPreference walking,
                                DayStartPreference dayStart,
                                List<PlaceCategory> interests,
                                List<PlaceCategory> avoid,
                                List<String> notes) {

    public PreferenceProfile {
        Objects.requireNonNull(pace, "pace");
        Objects.requireNonNull(walking, "walking");
        Objects.requireNonNull(dayStart, "dayStart");
        interests = interests == null ? List.of() : List.copyOf(interests);
        avoid = avoid == null ? List.of() : List.copyOf(avoid);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    /** 입력이 없거나 해석에 실패했을 때 쓰는 기본값 (보통 강도). */
    public static PreferenceProfile defaults() {
        return new PreferenceProfile(TravelPace.NORMAL, WalkingPreference.NORMAL, DayStartPreference.NORMAL,
                List.of(), List.of(), List.of());
    }
}
