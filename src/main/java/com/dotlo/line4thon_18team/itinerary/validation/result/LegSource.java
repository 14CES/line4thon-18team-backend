package com.dotlo.line4thon_18team.itinerary.validation.result;

/** 이동시간을 어디서 얻었는지. */
public enum LegSource {
    /** Routes API 실제 조회값 */
    ROUTES_API,
    /** API 오류 또는 좌표 없음 → 직선거리·기본값으로 추정 ('추정' 표시) */
    ESTIMATED,
    /** 경로 없음 → 직선거리로 추정 ("이동 경로를 찾지 못했어요") */
    ROUTE_NOT_FOUND,
    /** 같은 건물 등 거의 같은 위치 → 조회 생략 */
    SAME_SPOT
}
