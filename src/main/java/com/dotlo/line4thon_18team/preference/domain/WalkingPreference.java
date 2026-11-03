package com.dotlo.line4thon_18team.preference.domain;

/** 걷기 선호. 일정 검증에서 도보/대중교통을 고르는 거리 기준이 된다. */
public enum WalkingPreference {
    LESS("덜 걷기"),
    NORMAL("보통 걷기"),
    MORE("많이 걸어도 OK");

    private final String label;

    WalkingPreference(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
