package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.model.OpeningHours;
import com.dotlo.line4thon_18team.itinerary.validation.model.WeeklyPeriod;
import com.dotlo.line4thon_18team.place.OpeningHours.Hours;
import com.dotlo.line4thon_18team.place.OpeningHours.Range;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** BE2 영업시간(요일별 분 단위 구간)을 검증기 영업시간으로 바꾸는 부분. */
class SavedPlaceInfoProviderTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    @Test
    void 하루_영업시간을_옮긴다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(
                days(Map.of(DayOfWeek.MONDAY, List.of(new Range(10 * 60, 22 * 60)))));

        assertThat(hours.periods()).containsExactly(
                new WeeklyPeriod(DayOfWeek.MONDAY, LocalTime.of(10, 0), DayOfWeek.MONDAY, LocalTime.of(22, 0)));
        assertThat(hours.describe(MONDAY)).isEqualTo("10:00-22:00");
    }

    @Test
    void 브레이크_타임은_구간_두_개가_된다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(days(Map.of(
                DayOfWeek.MONDAY, List.of(new Range(11 * 60, 14 * 60), new Range(17 * 60, 22 * 60)))));

        assertThat(hours.describe(MONDAY)).isEqualTo("11:00-14:00, 17:00-22:00");
    }

    @Test
    void 자정을_넘기면_다음_날까지로_본다() {
        // 월 17:00 ~ 화 02:00 → [1020, 1560]
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(
                days(Map.of(DayOfWeek.MONDAY, List.of(new Range(17 * 60, 26 * 60)))));

        assertThat(hours.periods()).containsExactly(
                new WeeklyPeriod(DayOfWeek.MONDAY, LocalTime.of(17, 0), DayOfWeek.TUESDAY, LocalTime.of(2, 0)));
        assertThat(hours.covers(MONDAY.atTime(23, 0), MONDAY.plusDays(1).atTime(1, 0))).isTrue();
    }

    @Test
    void 휴무일은_그날_영업_구간이_없다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(
                days(Map.of(DayOfWeek.TUESDAY, List.of(new Range(10 * 60, 18 * 60)))));

        assertThat(hours.isClosedOn(MONDAY)).isTrue();
        assertThat(hours.isClosedOn(MONDAY.plusDays(1))).isFalse();
    }

    @Test
    void _24시간_영업은_언제나_열려_있다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(new Hours(true, Map.of()));

        assertThat(hours.isAlwaysOpen()).isTrue();
        LocalDateTime dawn = MONDAY.atTime(3, 0);
        assertThat(hours.earliestStart(dawn, 60)).contains(dawn);
    }

    // BE2 Hours 는 모든 요일 키를 채워서 준다 (휴무인 요일은 빈 리스트)
    private static Hours days(Map<DayOfWeek, List<Range>> open) {
        Map<DayOfWeek, List<Range>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, new ArrayList<>(open.getOrDefault(day, List.of())));
        }
        return new Hours(false, days);
    }
}
