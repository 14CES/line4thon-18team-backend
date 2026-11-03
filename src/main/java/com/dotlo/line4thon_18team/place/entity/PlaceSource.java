package com.dotlo.line4thon_18team.place.entity;

/**
 * 장소를 어디서 가져왔는지.
 */
public enum PlaceSource {
    TAKEOUT,        // Takeout 파일 업로드
    SHARED_LINK,    // 공유 URL
    MANUAL_SEARCH,  // 직접 검색해서 추가
    AI_RECOMMEND,   // AI 일정 생성·장소 추가에서 추천
    POPULAR         // 인기 코스에서 담기
}
