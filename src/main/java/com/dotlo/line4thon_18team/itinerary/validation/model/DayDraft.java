package com.dotlo.line4thon_18team.itinerary.validation.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** 하루 일정 (방문 순서대로). */
public record DayDraft(LocalDate date, List<VisitDraft> visits) {

    public DayDraft {
        Objects.requireNonNull(date, "date");
        visits = visits == null ? List.of() : List.copyOf(visits);
    }

    public static DayDraft of(LocalDate date, VisitDraft... visits) {
        return new DayDraft(date, List.of(visits));
    }
}
