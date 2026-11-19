package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.port.TravelLookup;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelQuery;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이동시간 조회 결과를 잠시 기억해 둔다.
 *
 * <p>검증기는 구간마다 구글 경로 API를 차례로 부르는데(구간당 최대 3번), 장소 추가와 재시도는 이미 검증한
 * 일정을 다시 검증한다. 같은 조회(출발·도착·수단·출발 시각이 모두 같음)는 구글을 다시 부르지 않고 기억한 값을 쓴다.
 *
 * <ul>
 *   <li>조회에 성공한 결과(추정치 포함)만 저장한다. 경로 없음·오류는 저장하지 않는다.</li>
 *   <li>기한이 지나면 다시 조회한다. 일시적인 오류로 생긴 추정치가 오래 남지 않게 하려는 것이다.</li>
 *   <li>서버 안에서만 기억한다. 서버를 다시 켜면 비워진다.</li>
 * </ul>
 */
public class CachedTravelTimeProvider implements TravelTimeProvider {

    /** 이만큼 쌓이면 통째로 비운다. 일정 수백 개를 검증해도 넘지 않는 크기다. */
    private static final int MAX_ENTRIES = 2000;

    private final TravelTimeProvider delegate;
    private final Duration ttl;
    private final Clock clock;
    private final Map<TravelQuery, Entry> cache = new ConcurrentHashMap<>();

    public CachedTravelTimeProvider(TravelTimeProvider delegate, Duration ttl) {
        this(delegate, ttl, Clock.systemUTC());
    }

    CachedTravelTimeProvider(TravelTimeProvider delegate, Duration ttl, Clock clock) {
        this.delegate = delegate;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public TravelLookup lookup(TravelQuery query) {
        Instant now = clock.instant();
        Entry hit = cache.get(query);
        if (hit != null && hit.expiresAt().isAfter(now)) {
            return hit.lookup();
        }

        TravelLookup lookup = delegate.lookup(query);
        if (lookup != null && lookup.status() == TravelLookup.Status.OK) {
            if (cache.size() >= MAX_ENTRIES) {
                cache.clear();
            }
            cache.put(query, new Entry(lookup, now.plus(ttl)));
        }
        return lookup;
    }

    private record Entry(TravelLookup lookup, Instant expiresAt) {
    }
}
