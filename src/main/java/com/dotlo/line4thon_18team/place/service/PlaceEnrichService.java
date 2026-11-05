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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    // ponytail: 장소마다 구글 검색 1번씩 순서대로 호출. 장소가 많아 느리면 병렬 호출로 바꾸기
    // 트랜잭션은 장소 1개 저장할 때만 연다. 구글을 기다리는 동안 DB 연결을 잡지 않고,
    // 중간에 구글 오류가 나도 앞 장소 결과는 남는다 (남은 장소는 PENDING → 다시 호출하면 이어서 처리)
    public void enrich(Long userId, Long tripId) {
        Trip trip = tripService.findOwnedTrip(userId, tripId);   // 없는 여행·남의 여행이면 404
        City city = CityController.findByName(trip.getCity())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_CITY));

        for (SavedPlace p : savedPlaceRepository.findAllByTripIdAndMatchStatus(tripId, PlaceMatchStatus.PENDING)) {
            PlaceInput input = new PlaceInput(p.getId(), p.getName(), p.getFeatureId(), p.getLatitude(), p.getLongitude());
            MatchResult r = placeMatcher.matchPlaces(List.of(input), city.lat(), city.lng()).get(0);
            transactionTemplate.executeWithoutResult(status -> save(tripId, city, r));
        }
    }

    private void save(Long tripId, City city, MatchResult r) {
        SavedPlace place = savedPlaceRepository.findById(r.id()).orElse(null);
        // 보강 도중 사용자가 삭제했거나, 동시에 들어온 다른 보강 요청이 먼저 처리한 장소.
        // 처리된 장소는 placeId 가 있어서, 그냥 진행하면 아래 중복 확인에서 자기 자신을 찾아 지운다
        if (place == null || place.getMatchStatus() != PlaceMatchStatus.PENDING) return;
        if (r.status() != PlaceMatcher.MatchStatus.MATCHED) {
            place.updateMatchStatus(PlaceMatchStatus.valueOf(r.status().name()));   // NEEDS_REVIEW / NOT_FOUND
            return;
        }
        Candidate c = r.place();
        // 같은 가게를 장소로도 핀으로도 저장했으면 리스트 이름만 합치고 이 행은 지운다 (업로드 중복 처리와 같은 방식)
        SavedPlace same = savedPlaceRepository.findFirstByTripIdAndPlaceId(tripId, c.placeId()).orElse(null);
        if (same != null) {
            place.getListNames().forEach(same::addListName);
            savedPlaceRepository.delete(place);
            return;
        }
        PlaceMatchStatus status = r.closed() ? PlaceMatchStatus.CLOSED
                : PlaceMatcher.meters(city.lat(), city.lng(), c.lat(), c.lng()) > CITY_LIMIT_M ? PlaceMatchStatus.OUT_OF_CITY
                : PlaceMatchStatus.MATCHED;
        place.applyMatch(c.placeId(), c.address(), c.lat(), c.lng(), r.category(), status);
    }
}
