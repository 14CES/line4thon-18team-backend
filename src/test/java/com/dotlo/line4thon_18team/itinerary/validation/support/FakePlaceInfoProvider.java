package com.dotlo.line4thon_18team.itinerary.validation.support;

import com.dotlo.line4thon_18team.itinerary.validation.model.PlaceInfo;
import com.dotlo.line4thon_18team.itinerary.validation.port.PlaceInfoProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 가짜 장소 정보 (BE2 대신). 등록하지 않은 placeId는 결과에서 빠진다. */
public final class FakePlaceInfoProvider implements PlaceInfoProvider {

    private final Map<String, PlaceInfo> places = new HashMap<>();

    public FakePlaceInfoProvider(List<PlaceInfo> places) {
        places.forEach(this::add);
    }

    public FakePlaceInfoProvider add(PlaceInfo place) {
        places.put(place.placeId(), place);
        return this;
    }

    @Override
    public Map<String, PlaceInfo> findPlaces(Collection<String> placeIds) {
        Map<String, PlaceInfo> found = new HashMap<>();
        for (String placeId : placeIds) {
            PlaceInfo place = places.get(placeId);
            if (place != null) {
                found.put(placeId, place);
            }
        }
        return found;
    }
}
