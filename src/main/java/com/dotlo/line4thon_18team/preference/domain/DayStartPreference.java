package com.dotlo.line4thon_18team.preference.domain;

/** 하루 일정 시작 시각 선호. */
public enum DayStartPreference {
    EARLY("아침 일찍 시작"),
    NORMAL("보통 시간에 시작"),
    LATE("느지막이 시작");

    private final String label;

    DayStartPreference(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
