package com.dotlo.line4thon_18team.itinerary.validation.result;

/** 문제의 심각도. */
public enum Severity {
    /** AI가 만든 일정에서는 허용하지 않는다 → 재배치·재생성 대상 */
    ERROR,
    /** 허용하되 화면에 표시한다 ('확인 필요') */
    WARNING
}
