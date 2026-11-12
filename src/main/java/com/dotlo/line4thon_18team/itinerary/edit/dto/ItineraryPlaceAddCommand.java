package com.dotlo.line4thon_18team.itinerary.edit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 장소 하나를 일정에 넣는 요청 바디.
 *
 * @param placeId 구글 장소 ID. 투표 1위 장소나 장소 검색에서 고른 장소를 보낸다.
 *                저장 장소에 없으면 서버가 구글에서 받아 담은 뒤 일정에 넣는다
 * @param note    넣을 자리에 대한 사용자 요청 (선택). 예: "저녁에 가고 싶어요", "2일차에 넣어 주세요"
 */
public record ItineraryPlaceAddCommand(
        @NotBlank(message = "placeId는 필수입니다.")
        @Size(max = 100, message = "placeId가 너무 깁니다.")
        String placeId,

        @Size(max = 200, message = "요청은 200자 이하로 입력해 주세요.")
        String note
) {
}
