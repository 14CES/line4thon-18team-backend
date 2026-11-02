package com.dotlo.line4thon_18team.itinerary.validation;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.preference.domain.DayStartPreference;
import com.dotlo.line4thon_18team.preference.domain.TravelPace;
import com.dotlo.line4thon_18team.preference.domain.WalkingPreference;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 일정 계산 규칙 값. 모두 가정값이라 기획과 함께 조정한다.
 *
 * <ul>
 *   <li>하루 시작: 이른 08:30 / 보통 09:30 / 느지막이 11:00, 하루 끝(숙소 복귀) 21:30</li>
 *   <li>여유시간(방문 사이): 여유롭게 20분 / 보통 10분 / 알차게 5분</li>
 *   <li>체류시간: 장소 유형별 기본값 × 강도 배율(1.2 / 1.0 / 0.85), 5분 단위 반올림</li>
 *   <li>도보 기준 직선거리: 덜 걷기 600m / 보통 1,000m / 많이 1,600m (넘으면 대중교통)</li>
 *   <li>공항: 입국 60분, 숙소 짐 정리 30분, 출국 120분 전 공항 도착, 짐 찾기 30분, 이동 기본 60분</li>
 *   <li>추정: 우회 계수 1.3, 도보 분당 75m, 대중교통 분당 333m(약 20km/h) + 대기 10분</li>
 * </ul>
 */
public final class ValidationPolicy {

    private final Map<DayStartPreference, LocalTime> dayStart;
    private final LocalTime dayEnd;
    private final Map<TravelPace, Integer> bufferMinutes;
    private final Map<TravelPace, Double> stayMultiplier;
    private final Map<PlaceCategory, Integer> defaultStayMinutes;
    private final Map<WalkingPreference, Integer> walkThresholdMeters;
    private final int farFromHotelMinutes;
    private final int longWaitMinutes;
    private final int arrivalProcessingMinutes;
    private final int settleMinutes;
    private final int departureCheckinMinutes;
    private final int luggagePickupMinutes;
    private final int defaultAirportTransferMinutes;
    private final int unknownLegMinutes;
    private final int sameSpotMeters;
    private final double detourFactor;
    private final double walkMetersPerMinute;
    private final double transitMetersPerMinute;
    private final int transitOverheadMinutes;

    private ValidationPolicy(Builder b) {
        this.dayStart = Map.copyOf(b.dayStart);
        this.dayEnd = b.dayEnd;
        this.bufferMinutes = Map.copyOf(b.bufferMinutes);
        this.stayMultiplier = Map.copyOf(b.stayMultiplier);
        this.defaultStayMinutes = Map.copyOf(b.defaultStayMinutes);
        this.walkThresholdMeters = Map.copyOf(b.walkThresholdMeters);
        this.farFromHotelMinutes = b.farFromHotelMinutes;
        this.longWaitMinutes = b.longWaitMinutes;
        this.arrivalProcessingMinutes = b.arrivalProcessingMinutes;
        this.settleMinutes = b.settleMinutes;
        this.departureCheckinMinutes = b.departureCheckinMinutes;
        this.luggagePickupMinutes = b.luggagePickupMinutes;
        this.defaultAirportTransferMinutes = b.defaultAirportTransferMinutes;
        this.unknownLegMinutes = b.unknownLegMinutes;
        this.sameSpotMeters = b.sameSpotMeters;
        this.detourFactor = b.detourFactor;
        this.walkMetersPerMinute = b.walkMetersPerMinute;
        this.transitMetersPerMinute = b.transitMetersPerMinute;
        this.transitOverheadMinutes = b.transitOverheadMinutes;
    }

    public static ValidationPolicy defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public LocalTime dayStart(DayStartPreference preference) {
        return dayStart.get(preference);
    }

    public LocalTime dayEnd() {
        return dayEnd;
    }

    public int bufferMinutes(TravelPace pace) {
        return bufferMinutes.get(pace);
    }

    /** 장소 유형 기본 체류시간 × 강도 배율, 5분 단위 반올림 (최소 5분). */
    public int stayMinutes(PlaceCategory category, TravelPace pace) {
        double minutes = defaultStayMinutes.get(category) * stayMultiplier.get(pace);
        return (int) Math.max(5, Math.round(minutes / 5.0) * 5);
    }

    public int walkThresholdMeters(WalkingPreference preference) {
        return walkThresholdMeters.get(preference);
    }

    public int farFromHotelMinutes() {
        return farFromHotelMinutes;
    }

    public int longWaitMinutes() {
        return longWaitMinutes;
    }

    public int arrivalProcessingMinutes() {
        return arrivalProcessingMinutes;
    }

    public int settleMinutes() {
        return settleMinutes;
    }

    public int departureCheckinMinutes() {
        return departureCheckinMinutes;
    }

    public int luggagePickupMinutes() {
        return luggagePickupMinutes;
    }

    public int defaultAirportTransferMinutes() {
        return defaultAirportTransferMinutes;
    }

    public int unknownLegMinutes() {
        return unknownLegMinutes;
    }

    public int sameSpotMeters() {
        return sameSpotMeters;
    }

    public double detourFactor() {
        return detourFactor;
    }

    public double walkMetersPerMinute() {
        return walkMetersPerMinute;
    }

    public double transitMetersPerMinute() {
        return transitMetersPerMinute;
    }

    public int transitOverheadMinutes() {
        return transitOverheadMinutes;
    }

    public static final class Builder {

        private final Map<DayStartPreference, LocalTime> dayStart = new EnumMap<>(DayStartPreference.class);
        private LocalTime dayEnd = LocalTime.of(21, 30);
        private final Map<TravelPace, Integer> bufferMinutes = new EnumMap<>(TravelPace.class);
        private final Map<TravelPace, Double> stayMultiplier = new EnumMap<>(TravelPace.class);
        private final Map<PlaceCategory, Integer> defaultStayMinutes = new EnumMap<>(PlaceCategory.class);
        private final Map<WalkingPreference, Integer> walkThresholdMeters = new EnumMap<>(WalkingPreference.class);
        private int farFromHotelMinutes = 50;
        private int longWaitMinutes = 90;
        private int arrivalProcessingMinutes = 60;
        private int settleMinutes = 30;
        private int departureCheckinMinutes = 120;
        private int luggagePickupMinutes = 30;
        private int defaultAirportTransferMinutes = 60;
        private int unknownLegMinutes = 30;
        private int sameSpotMeters = 50;
        private double detourFactor = 1.3;
        private double walkMetersPerMinute = 75;
        private double transitMetersPerMinute = 333;
        private int transitOverheadMinutes = 10;

        private Builder() {
            dayStart.put(DayStartPreference.EARLY, LocalTime.of(8, 30));
            dayStart.put(DayStartPreference.NORMAL, LocalTime.of(9, 30));
            dayStart.put(DayStartPreference.LATE, LocalTime.of(11, 0));

            bufferMinutes.put(TravelPace.RELAXED, 20);
            bufferMinutes.put(TravelPace.NORMAL, 10);
            bufferMinutes.put(TravelPace.PACKED, 5);

            stayMultiplier.put(TravelPace.RELAXED, 1.2);
            stayMultiplier.put(TravelPace.NORMAL, 1.0);
            stayMultiplier.put(TravelPace.PACKED, 0.85);

            defaultStayMinutes.put(PlaceCategory.FOOD, 60);
            defaultStayMinutes.put(PlaceCategory.CAFE, 45);
            defaultStayMinutes.put(PlaceCategory.SIGHTSEEING, 90);
            defaultStayMinutes.put(PlaceCategory.SHOPPING, 60);
            defaultStayMinutes.put(PlaceCategory.NATURE, 60);
            defaultStayMinutes.put(PlaceCategory.CULTURE, 90);
            defaultStayMinutes.put(PlaceCategory.NIGHTLIFE, 60);
            defaultStayMinutes.put(PlaceCategory.ACTIVITY, 180);
            defaultStayMinutes.put(PlaceCategory.OTHER, 60);

            walkThresholdMeters.put(WalkingPreference.LESS, 600);
            walkThresholdMeters.put(WalkingPreference.NORMAL, 1000);
            walkThresholdMeters.put(WalkingPreference.MORE, 1600);
        }

        public Builder dayStart(DayStartPreference preference, LocalTime time) {
            dayStart.put(preference, Objects.requireNonNull(time));
            return this;
        }

        public Builder dayEnd(LocalTime time) {
            this.dayEnd = Objects.requireNonNull(time);
            return this;
        }

        public Builder bufferMinutes(TravelPace pace, int minutes) {
            bufferMinutes.put(pace, minutes);
            return this;
        }

        public Builder stayMultiplier(TravelPace pace, double multiplier) {
            stayMultiplier.put(pace, multiplier);
            return this;
        }

        public Builder defaultStayMinutes(PlaceCategory category, int minutes) {
            defaultStayMinutes.put(category, minutes);
            return this;
        }

        public Builder walkThresholdMeters(WalkingPreference preference, int meters) {
            walkThresholdMeters.put(preference, meters);
            return this;
        }

        public Builder farFromHotelMinutes(int minutes) {
            this.farFromHotelMinutes = minutes;
            return this;
        }

        public Builder longWaitMinutes(int minutes) {
            this.longWaitMinutes = minutes;
            return this;
        }

        public Builder arrivalProcessingMinutes(int minutes) {
            this.arrivalProcessingMinutes = minutes;
            return this;
        }

        public Builder settleMinutes(int minutes) {
            this.settleMinutes = minutes;
            return this;
        }

        public Builder departureCheckinMinutes(int minutes) {
            this.departureCheckinMinutes = minutes;
            return this;
        }

        public Builder luggagePickupMinutes(int minutes) {
            this.luggagePickupMinutes = minutes;
            return this;
        }

        public Builder defaultAirportTransferMinutes(int minutes) {
            this.defaultAirportTransferMinutes = minutes;
            return this;
        }

        public Builder unknownLegMinutes(int minutes) {
            this.unknownLegMinutes = minutes;
            return this;
        }

        public Builder sameSpotMeters(int meters) {
            this.sameSpotMeters = meters;
            return this;
        }

        public Builder estimation(double detourFactor, double walkMetersPerMinute,
                                  double transitMetersPerMinute, int transitOverheadMinutes) {
            this.detourFactor = detourFactor;
            this.walkMetersPerMinute = walkMetersPerMinute;
            this.transitMetersPerMinute = transitMetersPerMinute;
            this.transitOverheadMinutes = transitOverheadMinutes;
            return this;
        }

        public ValidationPolicy build() {
            return new ValidationPolicy(this);
        }
    }
}
