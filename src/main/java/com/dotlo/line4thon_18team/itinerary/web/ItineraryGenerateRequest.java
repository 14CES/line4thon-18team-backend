package com.dotlo.line4thon_18team.itinerary.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 일정 생성 요청 바디 (선택). "여행에 담을 장소를 골라주세요"에서 고른 장소만으로 만들 때 보낸다.
 *
 * @param savedPlaceIds 고른 저장 장소 id ({@code GET /api/trips/{tripId}/places}의 id). 빠지면 저장 장소 전체.
 *                      빈 목록은 고른 범위와 결과가 달라지므로 400
 */
public record ItineraryGenerateRequest(
        @Size(min = 1, message = "장소를 하나 이상 골라 주세요.")
        List<@NotNull Long> savedPlaceIds
) {
}
