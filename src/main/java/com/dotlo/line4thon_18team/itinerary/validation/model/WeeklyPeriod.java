package com.dotlo.line4thon_18team.itinerary.validation.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 주간 영업 구간 하나. Places API(New) regularOpeningHours.periods와 같은 구조다.
 * Google의 day 값은 0=일요일 ~ 6=토요일이므로 BE2에서 DayOfWeek로 바꿔서 넘긴다.
 *
 * <p>예: 월 18:00 ~ 화 02:00 (자정을 넘기는 영업), 토 00:00 ~ 일 00:00 (토요일 24시간)
 */
public record WeeklyPeriod(DayOfWeek openDay, LocalTime openTime, DayOfWeek closeDay, LocalTime closeTime) {

    public WeeklyPeriod {
        Objects.requireNonNull(openDay, "openDay");
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(closeDay, "closeDay");
        Objects.requireNonNull(closeTime, "closeTime");
    }

    /** date(= openDay 요일)에 문을 여는 실제 영업 구간. */
    public TimeWindow windowStartingOn(LocalDate date) {
        if (date.getDayOfWeek() != openDay) {
            throw new IllegalArgumentException(date + "은(는) " + openDay + "가 아닙니다");
        }
        int dayOffset = (closeDay.getValue() - openDay.getValue() + 7) % 7;
        if (dayOffset == 0 && !closeTime.isAfter(openTime)) {
            dayOffset = 7;
        }
        return new TimeWindow(date.atTime(openTime), date.plusDays(dayOffset).atTime(closeTime));
    }
}
