package com.dotlo.line4thon_18team.place.service;

import com.dotlo.line4thon_18team.city.CityController;
import com.dotlo.line4thon_18team.city.CityController.City;
import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.place.PlaceMatcher;
import com.dotlo.line4thon_18team.place.PlaceMatcher.Candidate;
import com.dotlo.line4thon_18team.place.PlaceMatcher.MatchResult;
import com.dotlo.line4thon_18team.place.PlaceMatcher.PlaceInput;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 보강 (BE2). 매칭 전(PENDING) 장소를 구글 장소와 매칭해서 placeId·주소·좌표·유형을 채운다.
 * 폐업이면 CLOSED, 여행 도시 중심 50km 밖이면 OUT_OF_CITY 로 표시해서 일정에서 뺀다.
 */
@Service
@RequiredArgsConstructor
public class PlaceEnrichService {

    private static final int CITY_LIMIT_M = 50_000;

    private final TripService tripService;
    private final PlaceMatcher placeMatcher;
    private final SavedPlaceRepository savedPlaceRepository;

    // ponytail: 장소마다 구글 검색 1번씩 순서대로 호출. 장소가 많아 느리면 병렬 호출로 바꾸기
    @Transactional
    public void enrich(Long userId, Long tripId) {
        Trip trip = tripService.findOwnedTrip(userId, tripId);   // 없는 여행·남의 여행이면 404
        City city = CityController.findByName(trip.getCity())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_CITY));

        Map<Long, SavedPlace> pending = savedPlaceRepository.findAllByTripIdAndMatchStatus(tripId, PlaceMatchStatus.PENDING)
                .stream().collect(Collectors.toMap(SavedPlace::getId, Function.identity()));
        List<PlaceInput> inputs = pending.values().stream()
                .map(p -> new PlaceInput(p.getId(), p.getName(), p.getFeatureId(), p.getLatitude(), p.getLongitude()))
                .toList();

        for (MatchResult r : placeMatcher.matchPlaces(inputs, city.lat(), city.lng())) {
            SavedPlace place = pending.get(r.id());
            if (r.status() != PlaceMatcher.MatchStatus.MATCHED) {
                place.updateMatchStatus(PlaceMatchStatus.valueOf(r.status().name()));   // NEEDS_REVIEW / NOT_FOUND
                continue;
            }
            Candidate c = r.place();
            PlaceMatchStatus status = r.closed() ? PlaceMatchStatus.CLOSED
                    : PlaceMatcher.meters(city.lat(), city.lng(), c.lat(), c.lng()) > CITY_LIMIT_M ? PlaceMatchStatus.OUT_OF_CITY
                    : PlaceMatchStatus.MATCHED;
            place.applyMatch(c.placeId(), c.address(), c.lat(), c.lng(), r.category(), status);
        }
    }
}
