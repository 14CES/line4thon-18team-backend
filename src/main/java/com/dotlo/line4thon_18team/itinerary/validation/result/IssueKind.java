package com.dotlo.line4thon_18team.itinerary.validation.result;

/** 화면 표시용 분류 (FE2 역할표: 일정 충돌 / 장소 정보 누락 상태 표시). */
public enum IssueKind {
    /** 일정 충돌: 영업시간 밖, 휴무일, 예약 시각 놓침, 오래 기다림, 하루 시간 초과 */
    SCHEDULE_CONFLICT,
    /** 정보 누락·불확실: 영업시간 미확인, 좌표 없음, 경로 없음, 이동시간 추정 */
    INFO_MISSING,
    /** 참고 안내: 숙소에서 멂 */
    ADVISORY
}
