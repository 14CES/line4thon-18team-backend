package com.dotlo.line4thon_18team.itinerary.validation.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningHoursTest {

    /** 2026-12-14 월요일 */
    private static final LocalDate MONDAY = LocalDate.of(2026, 12, 14);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);

    @Test
    void 테스트_날짜의_요일_확인() {
        assertThat(MONDAY.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void 영업_전에_도착하면_여는_시각에_시작한다() {
        OpeningHours hours = OpeningHours.daily(LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertThat(hours.earliestStart(at(MONDAY, 8, 0), 60)).contains(at(MONDAY, 9, 0));
        assertThat(hours.earliestStart(at(MONDAY, 10, 0), 60)).contains(at(MONDAY, 10, 0));
        assertThat(hours.earliestStart(at(MONDAY, 16, 0), 60)).contains(at(MONDAY, 16, 0));
    }

    @Test
    void 닫기_전에_머물_수_없으면_다음_날로_넘기지_않고_비운다() {
        OpeningHours hours = OpeningHours.daily(LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertThat(hours.earliestStart(at(MONDAY, 16, 30), 90)).isEmpty();
    }

    @Test
    void 브레이크타임이_있으면_같은_날_다음_구간으로_간다() {
        List<WeeklyPeriod> periods = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            periods.add(new WeeklyPeriod(day, LocalTime.of(11, 0), day, LocalTime.of(14, 0)));
            periods.add(new WeeklyPeriod(day, LocalTime.of(17, 0), day, LocalTime.of(22, 0)));
        }
        OpeningHours hours = OpeningHours.weekly(periods);

        assertThat(hours.earliestStart(at(MONDAY, 13, 30), 60)).contains(at(MONDAY, 17, 0));
        assertThat(hours.describe(MONDAY)).isEqualTo("11:00-14:00, 17:00-22:00");
    }

    @Test
    void 자정을_넘기는_영업을_처리한다() {
        OpeningHours bar = OpeningHours.daily(LocalTime.of(18, 0), LocalTime.of(2, 0));

        assertThat(bar.earliestStart(at(MONDAY, 23, 0), 60)).contains(at(MONDAY, 23, 0));
        assertThat(bar.earliestStart(at(TUESDAY, 1, 0), 30)).contains(at(TUESDAY, 1, 0));
        assertThat(bar.earliestStart(at(TUESDAY, 1, 30), 60)).contains(at(TUESDAY, 18, 0));
        assertThat(bar.describe(MONDAY)).isEqualTo("18:00-02:00");
    }

    @Test
    void 휴무_요일을_판단한다() {
        OpeningHours cafe = OpeningHours.dailyExcept(LocalTime.of(11, 0), LocalTime.of(19, 0), Set.of(DayOfWeek.MONDAY));

        assertThat(cafe.isClosedOn(MONDAY)).isTrue();
        assertThat(cafe.isClosedOn(TUESDAY)).isFalse();
        assertThat(cafe.earliestStart(at(MONDAY, 12, 0), 45)).isEmpty();
        assertThat(cafe.describe(MONDAY)).isEqualTo("휴무");
    }

    @Test
    void 전날_밤_영업이_이어져도_그날_문을_안_열면_휴무다() {
        OpeningHours mondayNightOnly = OpeningHours.weekly(List.of(
                new WeeklyPeriod(DayOfWeek.MONDAY, LocalTime.of(20, 0), DayOfWeek.TUESDAY, LocalTime.of(3, 0))));

        assertThat(mondayNightOnly.isClosedOn(TUESDAY)).isTrue();
        assertThat(mondayNightOnly.earliestStart(at(TUESDAY, 1, 0), 60)).contains(at(TUESDAY, 1, 0));
    }

    @Test
    void 연중무휴_24시간() {
        OpeningHours always = OpeningHours.alwaysOpen();

        assertThat(always.earliestStart(at(MONDAY, 3, 0), 600)).contains(at(MONDAY, 3, 0));
        assertThat(always.isClosedOn(MONDAY)).isFalse();
        assertThat(always.covers(at(MONDAY, 23, 0), at(TUESDAY, 1, 0))).isTrue();
        assertThat(always.describe(MONDAY)).isEqualTo("24시간");
    }

    @Test
    void 특정_요일_24시간_영업과_주말을_넘는_구간() {
        WeeklyPeriod saturdayAllDay = new WeeklyPeriod(DayOfWeek.SATURDAY, LocalTime.MIDNIGHT, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT);
        LocalDate saturday = LocalDate.of(2026, 12, 19);

        TimeWindow window = saturdayAllDay.windowStartingOn(saturday);

        assertThat(window.start()).isEqualTo(at(saturday, 0, 0));
        assertThat(window.end()).isEqualTo(at(saturday.plusDays(1), 0, 0));
        WeeklyPeriod sundayToMonday = new WeeklyPeriod(DayOfWeek.SUNDAY, LocalTime.of(22, 0), DayOfWeek.MONDAY, LocalTime.of(3, 0));
        assertThat(sundayToMonday.windowStartingOn(saturday.plusDays(1)).end()).isEqualTo(at(saturday.plusDays(2), 3, 0));
    }

    @Test
    void 예약_시각이_영업시간_안인지_확인한다() {
        OpeningHours hours = OpeningHours.daily(LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertThat(hours.covers(at(MONDAY, 10, 0), at(MONDAY, 11, 0))).isTrue();
        assertThat(hours.covers(at(MONDAY, 16, 30), at(MONDAY, 17, 30))).isFalse();
    }

    @Test
    void 영업시간_정보가_같으면_같은_값이다() {
        assertThat(OpeningHours.daily(LocalTime.of(9, 0), LocalTime.of(17, 0)))
                .isEqualTo(OpeningHours.daily(LocalTime.of(9, 0), LocalTime.of(17, 0)));
        Optional<LocalDateTime> none = OpeningHours.weekly(List.of()).earliestStart(at(MONDAY, 9, 0), 10);
        assertThat(none).isEmpty();
    }

    private static LocalDateTime at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute);
    }
}
