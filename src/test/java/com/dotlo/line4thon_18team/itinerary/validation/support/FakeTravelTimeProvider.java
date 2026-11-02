package com.dotlo.line4thon_18team.itinerary.validation.support;

import com.dotlo.line4thon_18team.itinerary.validation.TravelEstimator;
import com.dotlo.line4thon_18team.itinerary.validation.model.GeoPoint;
import com.dotlo.line4thon_18team.itinerary.validation.model.TravelMode;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelLookup;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelQuery;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 가짜 이동시간 (BE2 Routes API 대신).
 * 기본은 모든 구간 같은 시간이고, 특정 구간만 경로 없음·오류·다른 시간으로 바꿀 수 있다. 받은 조회를 기록한다.
 */
public class FakeTravelTimeProvider implements TravelTimeProvider {

    private record Leg(GeoPoint from, GeoPoint to) {
    }

    private final Integer fixedMinutes;
    private final Map<Leg, TravelLookup> overrides = new HashMap<>();
    private final List<TravelQuery> queries = new ArrayList<>();
    private RuntimeException failure;

    private FakeTravelTimeProvider(Integer fixedMinutes) {
        this.fixedMinutes = fixedMinutes;
    }

    /** 모든 구간 minutes분. */
    public static FakeTravelTimeProvider fixedMinutes(int minutes) {
        return new FakeTravelTimeProvider(minutes);
    }

    /** 거리 비례 (도보 분당 80m, 대중교통 분당 400m + 8분). */
    public static FakeTravelTimeProvider byDistance() {
        return new FakeTravelTimeProvider(null);
    }

    public FakeTravelTimeProvider override(GeoPoint from, GeoPoint to, TravelLookup result) {
        overrides.put(new Leg(from, to), result);
        return this;
    }

    public FakeTravelTimeProvider failingWith(RuntimeException exception) {
        this.failure = exception;
        return this;
    }

    public List<TravelQuery> queries() {
        return queries;
    }

    @Override
    public TravelLookup lookup(TravelQuery query) {
        queries.add(query);
        if (failure != null) {
            throw failure;
        }
        TravelLookup override = overrides.get(new Leg(query.from(), query.to()));
        if (override != null) {
            return override;
        }
        double meters = TravelEstimator.straightLineMeters(query.from(), query.to());
        if (fixedMinutes != null) {
            return TravelLookup.ok(fixedMinutes, (int) Math.round(meters));
        }
        int minutes = query.mode() == TravelMode.WALK
                ? (int) Math.ceil(meters / 80)
                : 8 + (int) Math.ceil(meters / 400);
        return TravelLookup.ok(minutes, (int) Math.round(meters));
    }
}
