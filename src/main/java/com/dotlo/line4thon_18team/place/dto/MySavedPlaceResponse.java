package com.dotlo.line4thon_18team.place.dto;

import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.trip.entity.Trip;

import java.time.LocalDate;

/**
 * 마이·찜 목록의 "저장한 장소" 하나. 저장 장소는 여행마다 따로라서 어느 여행에 담겼는지 같이 준다.
 * 하트를 풀면 DELETE /api/trips/{trip.id}/places/{place.id} 로 그 여행에서 지운다.
 */
public record MySavedPlaceResponse(TripInfo trip, SavedPlaceResponse place) {

    public record TripInfo(Long id, String city, LocalDate startDate, LocalDate endDate) {
    }

    public static MySavedPlaceResponse from(SavedPlace place) {
        Trip trip = place.getTrip();
        return new MySavedPlaceResponse(
                new TripInfo(trip.getId(), trip.getCity(), trip.getStartDate(), trip.getEndDate()),
                SavedPlaceResponse.from(place));
    }
}
