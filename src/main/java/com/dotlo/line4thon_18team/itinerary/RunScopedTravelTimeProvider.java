package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.model.GeoPoint;
import com.dotlo.line4thon_18team.itinerary.validation.model.TravelMode;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelLookup;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelQuery;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 검증 한 판(ValidationRun) 안에서만 이동시간 조회 결과를 기억한다.
 *
 * <p>일정 생성·수정은 AI가 고친 일정을 최대 3번 검증하는데, 고치지 않은 구간은 출발·도착·수단·출발 시각이
 * 그대로라서 같은 조회가 반복된다. 그 구간은 구글 경로 API를 다시 부르지 않고 이번 요청에서 받은 값을 쓴다.
 *
 * <ul>
 *   <li>요청이 끝나면 이 객체와 함께 버려진다. 요청 사이에는 아무것도 남기지 않는다
 *       (구글 Routes API 약관은 좌표 외 경로 결과의 저장·캐시를 허용하지 않는다).</li>
 *   <li>조회에 성공한 결과(추정치 포함)만 기억한다. 경로 없음·오류는 다시 조회한다.</li>
 * </ul>
 */
public class RunScopedTravelTimeProvider implements TravelTimeProvider {

    private final TravelTimeProvider delegate;
    private final Map<TravelQuery, TravelLookup> lookups = new ConcurrentHashMap<>();
    private final Map<Leg, TravelLookup> byLeg = new ConcurrentHashMap<>();

    public RunScopedTravelTimeProvider(TravelTimeProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public TravelLookup lookup(TravelQuery query) {
        TravelLookup known = lookups.get(query);
        if (known != null) {
            return known;
        }
        TravelLookup lookup = delegate.lookup(query);
        if (lookup != null && lookup.status() == TravelLookup.Status.OK) {
            lookups.put(query, lookup);
            byLeg.putIfAbsent(Leg.of(query), lookup);
        }
        return lookup;
    }

    /**
     * 서버가 일정을 고쳐 보는 동안 쓸 조회. 이미 조회한 구간은 출발 시각이 달라도 그 값을 쓰고,
     * 조회하지 않은 구간은 구글을 부르지 않고 '조회 실패'로 둔다 (검증기가 직선거리로 추정한다).
     *
     * <p>방문 순서를 바꾸면 뒤쪽 구간의 출발 시각이 모두 달라져서 출발 시각까지 따지면 캐시가 전부 빗나간다.
     * 순서를 수백 번 바꿔 보는 동안 구글을 부르지 않으려고 출발 시각을 빼고 본다. 대중교통은 시각에 따라
     * 이동시간이 달라서 이 값은 근사치다. 고칠 후보를 고르는 데만 쓰고, 고친 일정은 원래 검증을 다시 통과해야 한다.
     */
    public TravelTimeProvider knownLegsOnly() {
        return query -> {
            TravelLookup known = byLeg.get(Leg.of(query));
            return known != null ? known : TravelLookup.error("조회하지 않음 (일정을 고쳐 보는 중)");
        };
    }

    /** 출발 시각을 뺀 이동 구간. */
    private record Leg(GeoPoint from, GeoPoint to, TravelMode mode, boolean chosen) {

        static Leg of(TravelQuery query) {
            return new Leg(query.from(), query.to(), query.mode(), query.chosen());
        }
    }
}
