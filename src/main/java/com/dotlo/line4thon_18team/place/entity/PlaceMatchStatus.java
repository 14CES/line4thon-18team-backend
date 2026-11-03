package com.dotlo.line4thon_18team.place.entity;

/**
 * 저장 장소의 매칭 상태.
 * MATCHED / NEEDS_REVIEW / NOT_FOUND 는 BE2 PlaceMatcher 결과와 같은 이름이다.
 */
public enum PlaceMatchStatus {
    PENDING,       // 아직 매칭 전
    MATCHED,       // place_id·좌표 확정
    NEEDS_REVIEW,  // 사용자가 후보 중 선택해야 함
    NOT_FOUND      // 검색 결과 없음
}
