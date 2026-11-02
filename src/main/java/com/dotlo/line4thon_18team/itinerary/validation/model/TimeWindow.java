package com.dotlo.line4thon_18team.itinerary.validation.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** 실제 날짜가 붙은 시간 구간 [start, end). */
public record TimeWindow(LocalDateTime start, LocalDateTime end) {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public TimeWindow {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("구간의 끝이 시작보다 늦어야 합니다: " + start + " ~ " + end);
        }
    }

    /** [from, to]가 이 구간 안에 들어가는지. */
    public boolean covers(LocalDateTime from, LocalDateTime to) {
        return !from.isBefore(start) && !to.isAfter(end);
    }

    /** 예: "18:00-02:00" */
    public String format() {
        return start.format(HH_MM) + "-" + end.format(HH_MM);
    }
}
