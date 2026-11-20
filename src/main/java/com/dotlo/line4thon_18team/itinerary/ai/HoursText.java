package com.dotlo.line4thon_18team.itinerary.ai;

import com.dotlo.line4thon_18team.itinerary.validation.model.OpeningHours;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AI에게 넘길 영업시간 문장. 여행 일차별로 그날 영업하는 시간대를 적는다 (요일이 아니라 일차 기준이라
 * 모델이 요일을 계산하지 않아도 된다).
 */
public final class HoursText {

    private HoursText() {
    }

    /**
     * 여행 기간의 일차별 영업시간 한 줄. 모든 날이 같으면 "매일 09:00-17:00", 다르면 "1일차 휴무 / 2일차 09:00-17:00".
     *
     * @return 영업시간을 모르면(hours가 null) null. 모르는 장소는 문장에 아무것도 붙이지 않는다
     */
    public static String forTrip(OpeningHours hours, LocalDate startDate, int dayCount) {
        if (hours == null) {
            return null;
        }
        List<String> perDay = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            perDay.add(hours.describe(startDate.plusDays(i)));
        }
        if (perDay.stream().distinct().count() == 1) {
            return "매일 " + perDay.get(0);
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < perDay.size(); i++) {
            parts.add((i + 1) + "일차 " + perDay.get(i));
        }
        return String.join(" / ", parts);
    }

    /** 여행 기간 내내 문을 열지 않는지. 영업시간을 모르면 열 수 있다고 본다. */
    public static boolean closedEveryDay(OpeningHours hours, LocalDate startDate, int dayCount) {
        if (hours == null || hours.isAlwaysOpen()) {
            return false;
        }
        for (int i = 0; i < dayCount; i++) {
            if (!hours.isClosedOn(startDate.plusDays(i))) {
                return false;
            }
        }
        return true;
    }
}
