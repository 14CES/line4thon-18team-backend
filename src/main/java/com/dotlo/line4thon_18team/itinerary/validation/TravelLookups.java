package com.dotlo.line4thon_18team.itinerary.validation;

import com.dotlo.line4thon_18team.itinerary.validation.port.TravelLookup;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelQuery;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;

/** BE2 함수가 예외를 던지거나 null을 돌려줘도 검증이 멈추지 않게 감싼다. */
final class TravelLookups {

    private TravelLookups() {
    }

    static TravelLookup safe(TravelTimeProvider provider, TravelQuery query) {
        try {
            TravelLookup lookup = provider.lookup(query);
            return lookup == null ? TravelLookup.error("결과 없음(null)") : lookup;
        } catch (RuntimeException e) {
            return TravelLookup.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
