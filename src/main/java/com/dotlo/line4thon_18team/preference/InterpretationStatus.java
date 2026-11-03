package com.dotlo.line4thon_18team.preference;

/** 취향 해석 결과 상태. */
public enum InterpretationStatus {
    /** AI가 해석했다. */
    INTERPRETED,
    /** 입력이 비어 있어 칩만으로 진행한다 (기능 상세: 빈 입력 → 칩만으로 진행). */
    EMPTY_INPUT,
    /** 해석에 실패해 기본값을 썼다 (기능 상세: 해석 실패 → 기본값(보통 강도) 적용). */
    FALLBACK_DEFAULT
}
