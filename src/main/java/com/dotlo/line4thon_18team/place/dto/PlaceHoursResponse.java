package com.dotlo.line4thon_18team.place.dto;

import com.dotlo.line4thon_18team.place.OpeningHours.Hours;
import com.dotlo.line4thon_18team.place.OpeningHours.Range;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 장소 상세 영업시간 ("09:00 ~ 18:00 요일별 ›", "● 지금 영업 중"). 시각은 여행 도시 현지 시간.
 *
 * @param openNow 지금 영업 중인지
 * @param days    월요일부터 일요일까지. 휴무인 요일은 hours 가 빈 목록, 브레이크 타임이 있으면 2개
 */
public record PlaceHoursResponse(boolean open24h, boolean openNow, List<Day> days) {

    public record Day(DayOfWeek day, List<Period> hours) {
    }

    /** 자정을 넘기면 close 가 open 보다 이르다 (17:00 ~ 02:00). 자정까지면 "24:00". */
    public record Period(String open, String close) {
    }

    public static PlaceHoursResponse of(Hours hours, ZonedDateTime now) {
        List<Day> days = hours.days().entrySet().stream()   // EnumMap 이라 월요일부터
                .map(e -> new Day(e.getKey(), e.getValue().stream().map(PlaceHoursResponse::period).toList()))
                .toList();
        boolean openNow = hours.isOpen(now.getDayOfWeek(), now.toLocalTime(), 1);
        return new PlaceHoursResponse(hours.open24h(), openNow, days);
    }

    private static Period period(Range range) {
        return new Period(time(range.start()), range.end() == 1440 ? "24:00" : time(range.end() % 1440));
    }

    private static String time(int minutes) {
        return "%02d:%02d".formatted(minutes / 60, minutes % 60);
    }
}
