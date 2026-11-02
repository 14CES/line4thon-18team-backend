package com.dotlo.line4thon_18team.itinerary.validation.model;

import com.dotlo.line4thon_18team.place.PlaceCategory;

/**
 * 검증에 필요한 장소 정보 (BE2 Places API 결과에서 만든다).
 *
 * @param location     좌표. 없으면 이동시간을 기본값으로 추정하고 '정보 누락'으로 표시한다.
 * @param category     장소 유형. 기본 체류시간을 정할 때 쓴다. null이면 OTHER.
 * @param openingHours 영업시간. null이면 '미확인'으로 표시한다 (기획: 데이터가 없으면 미확인 표시).
 */
public record PlaceInfo(String placeId, String name, GeoPoint location, PlaceCategory category,
                        OpeningHours openingHours) {

    public PlaceInfo {
        if (placeId == null || placeId.isBlank()) {
            throw new IllegalArgumentException("placeId가 비어 있습니다");
        }
        category = category == null ? PlaceCategory.OTHER : category;
    }
}
