package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.model.GeoPoint;
import com.dotlo.line4thon_18team.itinerary.validation.model.OpeningHours;
import com.dotlo.line4thon_18team.itinerary.validation.model.PlaceInfo;
import com.dotlo.line4thon_18team.itinerary.validation.model.WeeklyPeriod;
import com.dotlo.line4thon_18team.itinerary.validation.port.PlaceInfoProvider;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 저장 장소(BE1 saved_place)와 영업시간(BE2 place.OpeningHours)을 검증기가 쓰는 장소 정보로 바꾼다.
 *
 * <p>저장 장소는 여행 단위로 조회하므로 여행 하나를 검증할 때마다 새로 만든다.
 * 영업시간은 BE2가 Places API에서 바로 가져오고, 없거나 조회에 실패하면 '미확인'으로 둔다.
 */
class SavedPlaceInfoProvider implements PlaceInfoProvider {

    private static final Logger log = LoggerFactory.getLogger(SavedPlaceInfoProvider.class);

    private final Map<String, SavedPlace> byPlaceId;
    private final Function<String, com.dotlo.line4thon_18team.place.OpeningHours.Hours> hoursLookup;

    SavedPlaceInfoProvider(List<SavedPlace> places,
                           Function<String, com.dotlo.line4thon_18team.place.OpeningHours.Hours> hoursLookup) {
        this.hoursLookup = hoursLookup;
        Map<String, SavedPlace> map = new LinkedHashMap<>();
        for (SavedPlace place : places) {
            if (place.getPlaceId() != null) {   // 아직 매칭 전인 장소는 place ID가 없다
                map.putIfAbsent(place.getPlaceId(), place);
            }
        }
        this.byPlaceId = map;
    }

    @Override
    public Map<String, PlaceInfo> findPlaces(Collection<String> placeIds) {
        Map<String, PlaceInfo> found = new LinkedHashMap<>();
        for (String placeId : placeIds) {
            SavedPlace place = byPlaceId.get(placeId);
            if (place == null) {
                continue;   // 저장 장소에 없으면 검증기가 '장소 정보 없음'으로 표시한다
            }
            found.put(placeId, new PlaceInfo(placeId, place.getName(), location(place), place.getCategory(),
                    openingHours(placeId)));
        }
        return found;
    }

    private static GeoPoint location(SavedPlace place) {
        if (place.getLatitude() == null || place.getLongitude() == null) {
            return null;   // 매칭 전이거나 못 찾은 장소
        }
        return new GeoPoint(place.getLatitude(), place.getLongitude());
    }

    // 영업시간 정보가 없는 장소(거리, 동네 등)는 null, 조회 실패도 null('미확인')로 둔다.
    private OpeningHours openingHours(String placeId) {
        try {
            com.dotlo.line4thon_18team.place.OpeningHours.Hours hours = hoursLookup.apply(placeId);
            return hours == null ? null : toOpeningHours(hours);
        } catch (RuntimeException e) {
            log.warn("영업시간 조회 실패 placeId={}: {}", placeId, e.getMessage());
            return null;
        }
    }

    /** BE2 Hours(요일별 분 단위 구간) → 검증기 OpeningHours. */
    static OpeningHours toOpeningHours(com.dotlo.line4thon_18team.place.OpeningHours.Hours hours) {
        if (hours.open24h()) {
            return OpeningHours.alwaysOpen();
        }
        List<WeeklyPeriod> periods = new ArrayList<>();
        for (Map.Entry<DayOfWeek, List<com.dotlo.line4thon_18team.place.OpeningHours.Range>> entry
                : hours.days().entrySet()) {
            DayOfWeek openDay = entry.getKey();
            for (com.dotlo.line4thon_18team.place.OpeningHours.Range range : entry.getValue()) {
                // end 가 1440 을 넘으면 자정을 넘긴 영업 (예: 17:00~익일 02:00 → [1020, 1560])
                DayOfWeek closeDay = openDay.plus(range.end() / 1440);
                periods.add(new WeeklyPeriod(openDay, minutesToTime(range.start()),
                        closeDay, minutesToTime(range.end() % 1440)));
            }
        }
        return OpeningHours.weekly(periods);
    }

    private static LocalTime minutesToTime(int minutes) {
        return LocalTime.of(minutes / 60, minutes % 60);
    }
}
