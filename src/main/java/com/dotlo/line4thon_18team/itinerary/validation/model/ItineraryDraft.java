package com.dotlo.line4thon_18team.itinerary.validation.model;

import java.util.List;

/**
 * 검증 입력 일정. 일정 JSON 스키마가 BE1과 합의되면 그 스키마에서 이 형태로 바꿔 넣는다.
 */
public record ItineraryDraft(List<DayDraft> days) {

    public ItineraryDraft {
        days = days == null ? List.of() : List.copyOf(days);
    }

    public static ItineraryDraft of(DayDraft... days) {
        return new ItineraryDraft(List.of(days));
    }
}
