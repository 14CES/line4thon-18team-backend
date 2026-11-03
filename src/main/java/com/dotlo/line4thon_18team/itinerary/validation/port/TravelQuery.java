package com.dotlo.line4thon_18team.itinerary.validation.port;

import com.dotlo.line4thon_18team.itinerary.validation.model.GeoPoint;
import com.dotlo.line4thon_18team.itinerary.validation.model.TravelMode;

import java.time.ZonedDateTime;
import java.util.Objects;

/** 이동시간 조회 요청. 출발 시각은 대중교통 시간표 반영용이다. */
public record TravelQuery(GeoPoint from, GeoPoint to, TravelMode mode, ZonedDateTime departAt) {

    public TravelQuery {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(departAt, "departAt");
    }
}
