package com.dotlo.line4thon_18team.itinerary.validation.port;

import com.dotlo.line4thon_18team.itinerary.validation.model.TravelMode;

import java.util.Objects;

/**
 * 이동시간 조회 결과.
 *
 * @param durationMinutes OK일 때 이동시간(분)
 * @param distanceMeters  OK일 때 거리(m)
 * @param mode            OK일 때 실제 수단. null이면 요청한 수단 그대로 (수단을 직접 고르는 함수는 값을 채운다)
 * @param estimated       OK지만 실제 경로가 아니라 추정치인지 ('추정' 표시)
 * @param detail          실패 이유 (로그용)
 */
public record TravelLookup(Status status, int durationMinutes, int distanceMeters, TravelMode mode,
                           boolean estimated, String detail) {

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

    /** 요청한 수단으로 찾은 실제 경로. */
    public static TravelLookup ok(int durationMinutes, int distanceMeters) {
        return new TravelLookup(Status.OK, durationMinutes, distanceMeters, null, false, null);
    }

    /** 실제 경로. 수단은 이동시간 함수가 고른 값. */
    public static TravelLookup ok(int durationMinutes, int distanceMeters, TravelMode mode) {
        return new TravelLookup(Status.OK, durationMinutes, distanceMeters, Objects.requireNonNull(mode, "mode"),
                false, null);
    }

    /** 이동시간 함수가 준 추정치 (예: 대중교통 경로가 없어서 자동차 시간으로 추정). */
    public static TravelLookup estimated(int durationMinutes, int distanceMeters, TravelMode mode) {
        return new TravelLookup(Status.OK, durationMinutes, distanceMeters, Objects.requireNonNull(mode, "mode"),
                true, null);
    }

    public static TravelLookup noRoute() {
        return new TravelLookup(Status.NO_ROUTE, 0, 0, null, false, "경로 없음");
    }

    public static TravelLookup error(String detail) {
        return new TravelLookup(Status.ERROR, 0, 0, null, false, detail);
    }
}
