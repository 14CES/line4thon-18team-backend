package com.dotlo.line4thon_18team.itinerary.validation.model;

/** 검증할 일정이 어디서 왔는지. 일정 충돌의 심각도가 달라진다. */
public enum ValidationOrigin {
    /** AI 생성·AI 수정 결과 → 충돌은 ERROR (재배치·재생성) */
    AI_GENERATED,
    /** 사용자가 직접 편집 (드래그·삭제) → 충돌도 WARNING (기능 상세: 영업시간 밖으로 이동 → 경고만 표시하고 허용) */
    USER_EDITED
}
