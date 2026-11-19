package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.model.GeoPoint;
import com.dotlo.line4thon_18team.itinerary.validation.model.TravelMode;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelLookup;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelQuery;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CachedTravelTimeProviderTest {

    private static final GeoPoint NAMBA = new GeoPoint(34.6687, 135.5013);
    private static final GeoPoint OSAKA_CASTLE = new GeoPoint(34.6873, 135.5262);
    private static final ZonedDateTime DEPART = ZonedDateTime.of(2026, 12, 1, 9, 30, 0, 0, ZoneId.of("Asia/Tokyo"));
    private static final Duration TTL = Duration.ofMinutes(30);

    private final CountingProvider delegate = new CountingProvider(TravelLookup.ok(12, 1500, TravelMode.WALK));
    private final MutableClock clock = new MutableClock();
    private final CachedTravelTimeProvider provider = new CachedTravelTimeProvider(delegate, TTL, clock);

    @Test
    void 같은_조회는_한_번만_부른다() {
        TravelQuery query = new TravelQuery(NAMBA, OSAKA_CASTLE, TravelMode.TRANSIT, DEPART);

        TravelLookup first = provider.lookup(query);
        TravelLookup second = provider.lookup(query);

        assertThat(delegate.queries).hasSize(1);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void 출발_시각이나_수단이_다르면_따로_부른다() {
        provider.lookup(new TravelQuery(NAMBA, OSAKA_CASTLE, TravelMode.TRANSIT, DEPART));
        provider.lookup(new TravelQuery(NAMBA, OSAKA_CASTLE, TravelMode.TRANSIT, DEPART.plusHours(1)));
        provider.lookup(new TravelQuery(NAMBA, OSAKA_CASTLE, TravelMode.WALK, DEPART));
        provider.lookup(new TravelQuery(OSAKA_CASTLE, NAMBA, TravelMode.TRANSIT, DEPART));

        assertThat(delegate.queries).hasSize(4);
    }

    @Test
    void 기한이_지나면_다시_부른다() {
        TravelQuery query = new TravelQuery(NAMBA, OSAKA_CASTLE, TravelMode.TRANSIT, DEPART);

        provider.lookup(query);
        clock.advance(TTL.minusSeconds(1));
        provider.lookup(query);
        assertThat(delegate.queries).hasSize(1);   // 기한 안이라 그대로

        clock.advance(Duration.ofSeconds(2));
        provider.lookup(query);
        assertThat(delegate.queries).hasSize(2);   // 기한이 지나서 다시 조회
    }

    @Test
    void 추정치도_성공한_조회라_저장한다() {
        delegate.result = TravelLookup.estimated(35, 1500, TravelMode.TRANSIT);
        TravelQuery query = new TravelQuery(NAMBA, OSAKA_CASTLE, TravelMode.TRANSIT, DEPART);

        provider.lookup(query);
        TravelLookup again = provider.lookup(query);

        assertThat(delegate.queries).hasSize(1);
        assertThat(again.estimated()).isTrue();
    }

    @Test
    void 오류와_경로_없음은_저장하지_않는다() {
        TravelQuery query = new TravelQuery(NAMBA, OSAKA_CASTLE, TravelMode.TRANSIT, DEPART);

        delegate.result = TravelLookup.error("Routes API 오류");
        provider.lookup(query);
        provider.lookup(query);
        assertThat(delegate.queries).hasSize(2);   // 오류는 매번 다시 부른다

        delegate.result = TravelLookup.noRoute();
        provider.lookup(query);
        provider.lookup(query);
        assertThat(delegate.queries).hasSize(4);

        delegate.result = TravelLookup.ok(12, 1500, TravelMode.WALK);
        provider.lookup(query);   // 이번에 성공하면 그때부터 저장
        provider.lookup(query);
        assertThat(delegate.queries).hasSize(5);
    }

    private static class CountingProvider implements TravelTimeProvider {
        final List<TravelQuery> queries = new ArrayList<>();
        TravelLookup result;

        CountingProvider(TravelLookup result) {
            this.result = result;
        }

        @Override
        public TravelLookup lookup(TravelQuery query) {
            queries.add(query);
            return result;
        }
    }

    /** 시간을 직접 흘려 보낼 수 있는 시계 */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-19T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
