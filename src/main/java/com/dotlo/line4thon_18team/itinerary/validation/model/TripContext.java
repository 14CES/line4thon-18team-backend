package com.dotlo.line4thon_18team.itinerary.validation.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 검증에 필요한 여행 정보 (BE1 여행·항공·숙소 데이터에서 만든다). 시각은 모두 여행지 현지 시각이다.
 *
 * @param zone        여행지 시간대 (예: Asia/Tokyo). Routes API 출발 시각 계산에 쓴다.
 * @param hotel       숙소 좌표. 하루는 숙소에서 출발해 숙소로 돌아오는 것으로 계산한다.
 * @param airport     공항 좌표 (없으면 공항 이동시간 기본값 사용)
 * @param arrivalAt   첫날 항공 도착 시각 (없으면 첫날도 평소처럼 계산)
 * @param departureAt 마지막 날 항공 출발 시각 (없으면 마지막 날도 평소처럼 계산)
 */
public record TripContext(ZoneId zone,
                          LocalDate startDate,
                          LocalDate endDate,
                          GeoPoint hotel,
                          GeoPoint airport,
                          LocalDateTime arrivalAt,
                          LocalDateTime departureAt) {

    public TripContext {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(hotel, "hotel");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일이 시작일보다 빠릅니다: " + startDate + " ~ " + endDate);
        }
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
