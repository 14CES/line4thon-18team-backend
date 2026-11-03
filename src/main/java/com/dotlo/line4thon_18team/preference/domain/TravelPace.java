package com.dotlo.line4thon_18team.preference.domain;

/** 일정 강도. 해석에 실패하거나 입력이 없으면 NORMAL(보통 강도)을 쓴다. */
public enum TravelPace {
    RELAXED("여유롭게"),
    NORMAL("보통 강도"),
    PACKED("알차게");

    private final String label;

    TravelPace(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
