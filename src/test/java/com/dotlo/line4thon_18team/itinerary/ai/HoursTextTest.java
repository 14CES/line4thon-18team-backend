package com.dotlo.line4thon_18team.itinerary.ai;

import com.dotlo.line4thon_18team.itinerary.validation.model.OpeningHours;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HoursTextTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 12, 14);

    @Test
    void 모든_날이_같으면_매일로_적는다() {
        String text = HoursText.forTrip(OpeningHours.daily(LocalTime.of(9, 0), LocalTime.of(17, 0)), MONDAY, 3);

        assertThat(text).startsWith("매일 ").contains("09:00").contains("17:00");
    }

    @Test
    void 날마다_다르면_일차별로_적는다() {
        OpeningHours closedMonday = OpeningHours.dailyExcept(LocalTime.of(9, 0), LocalTime.of(17, 0),
                Set.of(DayOfWeek.MONDAY));

        String text = HoursText.forTrip(closedMonday, MONDAY, 2);

        assertThat(text).startsWith("1일차 휴무 / 2일차 ").contains("09:00");
    }

    @Test
    void 영업시간을_모르면_null이다() {
        assertThat(HoursText.forTrip(null, MONDAY, 3)).isNull();
    }

    @Test
    void 여행_내내_휴무인_장소만_닫은_것으로_본다() {
        OpeningHours closedMonday = OpeningHours.dailyExcept(LocalTime.of(9, 0), LocalTime.of(17, 0),
                Set.of(DayOfWeek.MONDAY));

        assertThat(HoursText.closedEveryDay(OpeningHours.weekly(List.of()), MONDAY, 4)).isTrue();
        assertThat(HoursText.closedEveryDay(closedMonday, MONDAY, 1)).isTrue();    // 월요일 하루뿐인 여행
        assertThat(HoursText.closedEveryDay(closedMonday, MONDAY, 2)).isFalse();   // 화요일엔 연다
    }

    @Test
    void 모르거나_24시간이면_닫은_것으로_보지_않는다() {
        assertThat(HoursText.closedEveryDay(null, MONDAY, 3)).isFalse();
        assertThat(HoursText.closedEveryDay(OpeningHours.alwaysOpen(), MONDAY, 3)).isFalse();
    }
}
