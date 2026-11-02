package com.dotlo.line4thon_18team.itinerary.validation.result;

import java.time.LocalDateTime;

/**
 * 계산된 방문 한 건.
 *
 * @param travel      직전 장소(첫 방문이면 숙소)에서 오는 이동
 * @param arrival     도착 시각
 * @param start       방문 시작 시각 (영업 시작·예약 시각까지 기다리면 arrival보다 늦다)
 * @param end         방문 종료 시각
 * @param waitMinutes 도착 후 기다린 시간
 * @param stayMinutes 체류시간
 */
public record ScheduledVisit(String itemId,
                             String placeId,
                             TravelLeg travel,
                             LocalDateTime arrival,
                             LocalDateTime start,
                             LocalDateTime end,
                             int waitMinutes,
                             int stayMinutes) {
}
