package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;

import java.util.List;

/**
 * AI가 만든 일정 (GEN-PLAN-001). 아직 저장하지 않은 상태다.
 *
 * @param draft            일차별 방문 순서 (itemId는 여기서 새로 만든 값)
 * @param validation       계산된 시각과 문제 목록
 * @param excludedPlaceIds 이번 일정에 넣지 못한 장소 (GEN-PLAN-001-E4 "12곳은 일정에 넣지 못했어요")
 * @param attempts         배치 시도 횟수 (1이면 처음에 바로 통과)
 */
public record GeneratedItinerary(ItineraryDraft draft,
                                 ValidationResult validation,
                                 List<String> excludedPlaceIds,
                                 int attempts) {

    public GeneratedItinerary {
        excludedPlaceIds = excludedPlaceIds == null ? List.of() : List.copyOf(excludedPlaceIds);
    }
}
