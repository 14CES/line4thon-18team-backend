package com.dotlo.line4thon_18team.place;

/**
 * 장소 유형. 취향(관심·제외 유형)과 일정 검증(기본 체류시간)이 함께 쓴다.
 *
 * <p>BE2가 Places API의 장소 type을 이 값으로 바꿔서 넘겨주는 것을 전제로 한다.
 * OTHER는 취향 칩으로는 쓰지 않는다.
 */
public enum PlaceCategory {
    FOOD("맛집"),
    CAFE("카페"),
    SIGHTSEEING("관광 명소"),
    SHOPPING("쇼핑"),
    NATURE("자연"),
    CULTURE("문화·역사"),
    NIGHTLIFE("야경·밤"),
    ACTIVITY("체험·테마파크"),
    OTHER("기타");

    private final String label;

    PlaceCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 취향 칩으로 고를 수 있는 유형인지. */
    public boolean selectableAsPreference() {
        return this != OTHER;
    }
}
