package com.dotlo.line4thon_18team.itinerary.validation.result;

import com.dotlo.line4thon_18team.itinerary.validation.model.TravelMode;

/** 이동 구간 하나. */
public record TravelLeg(TravelMode mode, int minutes, int distanceMeters, LegSource source) {

    /** 실제 조회값이 아닌지 ('추정' 표시 여부). */
    public boolean isEstimated() {
        return source == LegSource.ESTIMATED || source == LegSource.ROUTE_NOT_FOUND;
    }
}
