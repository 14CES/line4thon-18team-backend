package com.dotlo.line4thon_18team.itinerary.validation.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 장소 영업시간. 정보가 아예 없으면 PlaceInfo.openingHours를 null로 둔다 ('미확인').
 */
public final class OpeningHours {

    private static final OpeningHours ALWAYS_OPEN = new OpeningHours(true, List.of());

    private final boolean alwaysOpen;
    private final List<WeeklyPeriod> periods;

    private OpeningHours(boolean alwaysOpen, List<WeeklyPeriod> periods) {
        this.alwaysOpen = alwaysOpen;
        this.periods = List.copyOf(periods);
    }

    /** 24시간 연중무휴 (Places API: close 없는 period 하나). */
    public static OpeningHours alwaysOpen() {
        return ALWAYS_OPEN;
    }

    /** Places API periods를 그대로 옮긴 영업시간. 빈 목록이면 매일 휴무. */
    public static OpeningHours weekly(List<WeeklyPeriod> periods) {
        Objects.requireNonNull(periods, "periods");
        return new OpeningHours(false, periods);
    }

    /** 매일 같은 시간. close가 open보다 이르거나 같으면 다음 날 그 시각까지로 본다 (예: 18:00~02:00). */
    public static OpeningHours daily(LocalTime open, LocalTime close) {
        return dailyExcept(open, close, Set.of());
    }

    /** closedDays만 휴무, 나머지 요일은 같은 시간 영업. */
    public static OpeningHours dailyExcept(LocalTime open, LocalTime close, Set<DayOfWeek> closedDays) {
        List<WeeklyPeriod> periods = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (closedDays.contains(day)) {
                continue;
            }
            DayOfWeek closeDay = close.isAfter(open) ? day : day.plus(1);
            periods.add(new WeeklyPeriod(day, open, closeDay, close));
        }
        return new OpeningHours(false, periods);
    }

    public boolean isAlwaysOpen() {
        return alwaysOpen;
    }

    public List<WeeklyPeriod> periods() {
        return periods;
    }

    /** 그날 문을 여는 구간이 하나도 없는지 (전날 밤부터 이어지는 영업은 그날 영업으로 치지 않는다). */
    public boolean isClosedOn(LocalDate date) {
        return !alwaysOpen && windowsOpeningOn(date).isEmpty();
    }

    /** [from, to] 전체가 영업 구간 하나 안에 들어가는지 (예약 시각 확인용). */
    public boolean covers(LocalDateTime from, LocalDateTime to) {
        if (alwaysOpen) {
            return true;
        }
        return windowsAround(from.toLocalDate()).stream().anyMatch(window -> window.covers(from, to));
    }

    /**
     * arrival 이후 stayMinutes 동안 머물 수 있는 가장 이른 시작 시각.
     * 같은 날 문을 여는 구간(전날 밤부터 이어지는 구간 포함)만 본다. 다음 날로 넘기지 않는다.
     */
    public Optional<LocalDateTime> earliestStart(LocalDateTime arrival, int stayMinutes) {
        if (alwaysOpen) {
            return Optional.of(arrival);
        }
        LocalDateTime nextDay = arrival.toLocalDate().plusDays(1).atStartOfDay();
        for (TimeWindow window : windowsAround(arrival.toLocalDate())) {
            if (!window.start().isBefore(nextDay) || !window.end().isAfter(arrival)) {
                continue;
            }
            LocalDateTime start = window.start().isAfter(arrival) ? window.start() : arrival;
            if (!start.plusMinutes(stayMinutes).isAfter(window.end())) {
                return Optional.of(start);
            }
        }
        return Optional.empty();
    }

    /** 그날 영업시간 요약. 예: "11:00-14:00, 17:00-22:00", "24시간", "휴무" */
    public String describe(LocalDate date) {
        if (alwaysOpen) {
            return "24시간";
        }
        List<TimeWindow> windows = windowsOpeningOn(date);
        if (windows.isEmpty()) {
            return "휴무";
        }
        return windows.stream().map(TimeWindow::format).collect(Collectors.joining(", "));
    }

    private List<TimeWindow> windowsOpeningOn(LocalDate date) {
        List<TimeWindow> windows = new ArrayList<>();
        for (WeeklyPeriod period : periods) {
            if (period.openDay() == date.getDayOfWeek()) {
                windows.add(period.windowStartingOn(date));
            }
        }
        windows.sort(Comparator.comparing(TimeWindow::start));
        return windows;
    }

    /** date 전날~다음 날에 문을 여는 구간 (자정을 넘기는 영업을 놓치지 않으려고 넓게 본다). */
    private List<TimeWindow> windowsAround(LocalDate date) {
        List<TimeWindow> windows = new ArrayList<>();
        windows.addAll(windowsOpeningOn(date.minusDays(1)));
        windows.addAll(windowsOpeningOn(date));
        windows.addAll(windowsOpeningOn(date.plusDays(1)));
        windows.sort(Comparator.comparing(TimeWindow::start));
        return windows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OpeningHours other)) {
            return false;
        }
        return alwaysOpen == other.alwaysOpen && periods.equals(other.periods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alwaysOpen, periods);
    }

    @Override
    public String toString() {
        return alwaysOpen ? "OpeningHours[24시간]" : "OpeningHours" + periods;
    }
}
