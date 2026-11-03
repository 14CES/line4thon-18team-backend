package com.dotlo.line4thon_18team.itinerary.validation.result;

import com.dotlo.line4thon_18team.itinerary.validation.model.ValidationOrigin;

/**
 * 검증 문제 코드. 사용자 문구는 FE가 코드와 params로 만든다.
 */
public enum IssueCode {
    /** 영업시간 안에 머물 수 없음. params: arrival, stayMinutes, openingHours */
    OUTSIDE_OPENING_HOURS(IssueKind.SCHEDULE_CONFLICT),
    /** 그날 휴무. params: dayOfWeek */
    CLOSED_ON_DAY(IssueKind.SCHEDULE_CONFLICT),
    /** 예약 시각보다 늦게 도착. params: fixedStart, arrival, lateMinutes */
    FIXED_TIME_MISSED(IssueKind.SCHEDULE_CONFLICT),
    /** 문 열 때까지 오래 기다림. params: arrival, start, waitMinutes */
    LONG_WAIT(IssueKind.SCHEDULE_CONFLICT),
    /** 하루 끝 시각(숙소 복귀 기준)을 넘김. params: endsAt, dayEnd, overflowMinutes, availableMinutes */
    DAY_OVERFLOW(IssueKind.SCHEDULE_CONFLICT),

    /** 영업시간 정보 없음 → '미확인' */
    HOURS_UNKNOWN(IssueKind.INFO_MISSING),
    /** 장소 정보를 찾지 못함 */
    PLACE_INFO_MISSING(IssueKind.INFO_MISSING),
    /** 좌표 없음 → 이동시간 기본값 사용 */
    PLACE_LOCATION_MISSING(IssueKind.INFO_MISSING),
    /** 경로 없음 → "이동 경로를 찾지 못했어요". params: mode, estimatedMinutes */
    ROUTE_NOT_FOUND(IssueKind.INFO_MISSING),
    /** 경로 API 오류 → 직선거리 추정치 + '추정'. params: mode, estimatedMinutes */
    TRAVEL_ESTIMATED(IssueKind.INFO_MISSING),

    /** 숙소에서 첫 장소까지 멂 → "숙소에서 50분 거리예요". params: minutes */
    FAR_FROM_HOTEL(IssueKind.ADVISORY);

    private final IssueKind kind;

    IssueCode(IssueKind kind) {
        this.kind = kind;
    }

    public IssueKind kind() {
        return kind;
    }

    /**
     * 일정 충돌은 AI가 만든 일정이면 ERROR(재배치·재생성), 사용자가 직접 고친 일정이면 WARNING(경고만 표시하고 허용).
     * 정보 누락·안내는 항상 WARNING.
     */
    public Severity severityFor(ValidationOrigin origin) {
        return kind == IssueKind.SCHEDULE_CONFLICT && origin == ValidationOrigin.AI_GENERATED
                ? Severity.ERROR : Severity.WARNING;
    }
}
