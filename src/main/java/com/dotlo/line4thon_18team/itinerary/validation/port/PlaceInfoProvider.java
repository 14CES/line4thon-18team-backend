package com.dotlo.line4thon_18team.itinerary.validation.port;

import com.dotlo.line4thon_18team.itinerary.validation.model.PlaceInfo;

import java.util.Collection;
import java.util.Map;

/**
 * BE2에게 받을 장소 정보 함수. 저장 장소 보강 결과(DB)나 Places API에서 채운다.
 *
 * <p>합의할 점: 좌표·영업시간(주간 periods)·장소 유형(PlaceCategory)·휴무 반영 여부.
 */
public interface PlaceInfoProvider {

    /**
     * @return placeId별 장소 정보. 찾지 못한 placeId는 결과에서 빠진다 (검증기가 '정보 누락'으로 표시).
     */
    Map<String, PlaceInfo> findPlaces(Collection<String> placeIds);
}
