package com.dotlo.line4thon_18team.place.dto;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.PlaceDetails.Detail;

/**
 * 장소 상세 ("명소 / 산넨자카 / ★ 4.4 구글 리뷰 17,380 / 소개 / 주소 / 위치").
 *
 * @param summary      소개 한 줄. 구글에 없으면 null
 * @param savedPlaceId 이 여행에 저장한 장소면 저장 장소 id (하트 채움, 해제는 DELETE /api/trips/{tripId}/places/{id}). 아니면 null
 */
public record PlaceDetailResponse(String placeId, String name, PlaceCategory category, Double rating,
                                  Integer reviewCount, String summary, String address, double latitude,
                                  double longitude, String googleMapsUri, Long savedPlaceId) {

    public static PlaceDetailResponse of(Detail d, Long savedPlaceId) {
        return new PlaceDetailResponse(d.placeId(), d.name(), d.category(), d.rating(), d.reviewCount(), d.summary(),
                d.address(), d.lat(), d.lng(), d.googleMapsUri(), savedPlaceId);
    }
}
