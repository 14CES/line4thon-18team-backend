package com.dotlo.line4thon_18team.itinerary.validation.port;

import java.util.Objects;

/**
 * 이동시간 조회 결과.
 *
 * @param durationMinutes OK일 때 이동시간(분)
 * @param distanceMeters  OK일 때 경로 거리(m)
 * @param detail          실패 이유 (로그용)
 */
public record TravelLookup(Status status, int durationMinutes, int distanceMeters, String detail) {

    public enum Status {
        OK,
        /** 해당 수단으로 갈 수 있는 경로가 없다 */
        NO_ROUTE,
        /** API 오류·타임아웃·한도 초과 등 */
        ERROR
    }

    public TravelLookup {
        Objects.requireNonNull(status, "status");
        if (durationMinutes < 0 || distanceMeters < 0) {
            throw new IllegalArgumentException("이동시간·거리는 0 이상이어야 합니다");
        }
    }

    public static TravelLookup ok(int durationMinutes, int distanceMeters) {
        return new TravelLookup(Status.OK, durationMinutes, distanceMeters, null);
    }

    public static TravelLookup noRoute() {
        return new TravelLookup(Status.NO_ROUTE, 0, 0, "경로 없음");
    }

    public static TravelLookup error(String detail) {
        return new TravelLookup(Status.ERROR, 0, 0, detail);
    }
}
