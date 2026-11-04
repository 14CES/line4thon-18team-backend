package com.dotlo.line4thon_18team.place.service;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.place.dto.SavedPlaceListResponse;
import com.dotlo.line4thon_18team.place.dto.SavedPlaceResponse;
import com.dotlo.line4thon_18team.place.dto.TakeoutUploadResponse;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.place.takeout.TakeoutList;
import com.dotlo.line4thon_18team.place.takeout.TakeoutParser;
import com.dotlo.line4thon_18team.place.takeout.TakeoutPlace;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedPlaceService {

    private final TripService tripService;
    private final SavedPlaceRepository savedPlaceRepository;
    private final TakeoutParser takeoutParser;

    public SavedPlaceListResponse getPlaces(Long userId, Long tripId) {
        tripService.findOwnedTrip(userId, tripId);   // 없는 여행·남의 여행이면 404

        List<SavedPlaceResponse> places = savedPlaceRepository.findAllByTripIdOrderByIdAsc(tripId).stream()
                .map(SavedPlaceResponse::from)
                .toList();
        return new SavedPlaceListResponse(tripId, places.size(), places);
    }

    /**
     * Takeout CSV/zip 업로드.
     * 같은 장소 판별: feature ID 가 같으면 같은 장소, feature ID 가 없는 핀은 URL 이 같으면 같은 장소.
     * 같은 장소면 새로 저장하지 않고 리스트 이름만 추가한다.
     */
    @Transactional
    public TakeoutUploadResponse uploadTakeout(Long userId, Long tripId, MultipartFile file) {
        Trip trip = tripService.findOwnedTrip(userId, tripId);
        List<TakeoutList> lists = takeoutParser.parse(file);   // 파일 문제면 여기서 400

        Map<String, SavedPlace> byFeatureId = new HashMap<>();
        Map<String, SavedPlace> byUrl = new HashMap<>();
        for (SavedPlace place : savedPlaceRepository.findAllByTripIdOrderByIdAsc(tripId)) {
            if (place.getFeatureId() != null) {
                byFeatureId.put(place.getFeatureId(), place);
            } else if (place.getGoogleUrl() != null) {
                byUrl.put(place.getGoogleUrl(), place);
            }
        }

        List<TakeoutUploadResponse.ListResult> results = new ArrayList<>();
        int totalNewCount = 0;
        for (TakeoutList list : lists) {
            int newCount = 0;
            for (TakeoutPlace row : list.places()) {
                Map<String, SavedPlace> index = row.featureId() != null ? byFeatureId : byUrl;
                String key = row.featureId() != null ? row.featureId() : row.googleUrl();

                SavedPlace existing = index.get(key);
                if (existing != null) {
                    existing.addListName(list.listName());
                    continue;
                }
                SavedPlace created = savedPlaceRepository.save(SavedPlace.fromTakeout(trip, row.featureId(),
                        row.name(), row.note(), row.googleUrl(), row.latitude(), row.longitude(), list.listName()));
                index.put(key, created);   // 같은 파일 안 다른 리스트에서 또 나와도 중복으로 처리
                newCount++;
            }
            totalNewCount += newCount;
            results.add(new TakeoutUploadResponse.ListResult(list.listName(), list.places().size(),
                    newCount, list.places().size() - newCount, list.skippedRows()));
        }
        return new TakeoutUploadResponse(tripId, totalNewCount, results);
    }

    @Transactional
    public void deletePlace(Long userId, Long tripId, Long placeId) {
        tripService.findOwnedTrip(userId, tripId);   // 없는 여행·남의 여행이면 404

        SavedPlace place = savedPlaceRepository.findById(placeId)
                .filter(p -> p.getTrip().getId().equals(tripId))   // 다른 여행의 장소면 없는 것으로 본다
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_PLACE_NOT_FOUND));
        savedPlaceRepository.delete(place);
    }
}
