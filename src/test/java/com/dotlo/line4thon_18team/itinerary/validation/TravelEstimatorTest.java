package com.dotlo.line4thon_18team.itinerary.validation;

import com.dotlo.line4thon_18team.itinerary.validation.model.TravelMode;
import org.junit.jupiter.api.Test;

import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.HOTEL_NAMBA;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.OSAKA_CASTLE;
import static org.assertj.core.api.Assertions.assertThat;

class TravelEstimatorTest {

    private final TravelEstimator estimator = new TravelEstimator(ValidationPolicy.defaults());

    @Test
    void 직선거리를_계산한다() {
        double meters = TravelEstimator.straightLineMeters(HOTEL_NAMBA, OSAKA_CASTLE.location());

        assertThat(meters).isBetween(3200.0, 3450.0);
        assertThat(TravelEstimator.straightLineMeters(HOTEL_NAMBA, HOTEL_NAMBA)).isZero();
    }

    @Test
    void 도보와_대중교통_시간을_추정한다() {
        // 800m × 1.3 ÷ 80m/분 = 13.0 → 13분
        assertThat(estimator.estimateMinutes(800, TravelMode.WALK)).isEqualTo(13);
        // 10분 + 3,320m × 1.3 ÷ 300m/분 = 24.39 → 25분
        assertThat(estimator.estimateMinutes(3320, TravelMode.TRANSIT)).isEqualTo(25);
        assertThat(estimator.estimateMinutes(0, TravelMode.WALK)).isEqualTo(1);
        assertThat(estimator.estimateDistanceMeters(1000)).isEqualTo(1300);
    }
}
