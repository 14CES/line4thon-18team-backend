package com.dotlo.line4thon_18team.place.dto;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;

/**
 * 검색 화면 "○○에서 많이 담는 곳" 장소 하나. 사진은 /api/places/{placeId}/photos/0 으로 받는다.
 *
 * @param savedCount 이 장소를 담은 여행 수
 */
public record PopularPlaceResponse(
        String placeId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        PlaceCategory category,
        long savedCount
) {

    public static PopularPlaceResponse of(SavedPlace place, long savedCount) {
        return new PopularPlaceResponse(place.getPlaceId(), place.getName(), place.getAddress(),
                place.getLatitude(), place.getLongitude(), place.getCategory(), savedCount);
    }
}
