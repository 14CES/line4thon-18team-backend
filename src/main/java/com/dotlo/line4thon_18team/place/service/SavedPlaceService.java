package com.dotlo.line4thon_18team.place.service;

import com.dotlo.line4thon_18team.place.dto.SavedPlaceListResponse;
import com.dotlo.line4thon_18team.place.dto.SavedPlaceResponse;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedPlaceService {

    private final TripService tripService;
    private final SavedPlaceRepository savedPlaceRepository;

    public SavedPlaceListResponse getPlaces(Long userId, Long tripId) {
        tripService.findOwnedTrip(userId, tripId);   // 없는 여행·남의 여행이면 404

        List<SavedPlaceResponse> places = savedPlaceRepository.findAllByTripIdOrderByIdAsc(tripId).stream()
                .map(SavedPlaceResponse::from)
                .toList();
        return new SavedPlaceListResponse(tripId, places.size(), places);
    }
}
