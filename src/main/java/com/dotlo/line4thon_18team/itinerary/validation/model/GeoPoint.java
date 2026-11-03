package com.dotlo.line4thon_18team.itinerary.validation.model;

/** 위도·경도. */
public record GeoPoint(double lat, double lng) {

    public GeoPoint {
        if (!(lat >= -90 && lat <= 90) || !(lng >= -180 && lng <= 180)) {
            throw new IllegalArgumentException("좌표 범위를 벗어났습니다: " + lat + ", " + lng);
        }
    }
}
