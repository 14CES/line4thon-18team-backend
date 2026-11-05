package com.dotlo.line4thon_18team.place.takeout;

/**
 * Takeout CSV 한 행에서 꺼낸 장소.
 * featureId 가 있으면 좌표는 null, featureId 가 없으면(직접 찍은 핀 등) URL 에서 꺼낸 좌표가 있다.
 */
public record TakeoutPlace(
        String name,
        String note,
        String googleUrl,
        String featureId,
        Double latitude,
        Double longitude
) {
}
