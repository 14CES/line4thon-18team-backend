package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;

import java.util.List;

/**
 * AI가 만들어 저장까지 끝낸 일정 (GEN-PLAN-001).
 *
 * @param itinerary        저장된 버전. 최신 일정 조회(GET /itineraries/latest)와 같은 모양이다
 * @param excludedPlaceIds 이번 일정에 넣지 못한 장소 (GEN-PLAN-001-E4 "12곳은 일정에 넣지 못했어요")
 * @param closedPlaceIds   여행 기간 내내 영업하지 않아 일정에서 뺀 장소 ("영업하지 않아 제외했어요"). excludedPlaceIds와 겹치지 않는다
 */
public record GeneratedItineraryVersion(ItineraryVersionView itinerary, List<String> excludedPlaceIds,
                                        List<String> closedPlaceIds) {

    public GeneratedItineraryVersion(ItineraryVersionView itinerary, List<String> excludedPlaceIds) {
        this(itinerary, excludedPlaceIds, List.of());
    }

    public GeneratedItineraryVersion {
        excludedPlaceIds = excludedPlaceIds == null ? List.of() : List.copyOf(excludedPlaceIds);
        closedPlaceIds = closedPlaceIds == null ? List.of() : List.copyOf(closedPlaceIds);
    }
}
