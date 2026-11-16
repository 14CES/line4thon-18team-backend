package com.dotlo.line4thon_18team.place.controller;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.city.CityController;
import com.dotlo.line4thon_18team.city.CityController.City;
import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.place.OpeningHours;
import com.dotlo.line4thon_18team.place.OpeningHours.Hours;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.PlaceMatcher;
import com.dotlo.line4thon_18team.place.PlaceMatcher.Candidate;
import com.dotlo.line4thon_18team.place.dto.PlaceHoursResponse;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceSearchController {

    // 도시 밖 제외 기준과 같은 50km. Places API 검색 범위 최대값이기도 함
    private static final int SEARCH_RADIUS_M = 50_000;

    private final TripService tripService;
    private final PlaceMatcher placeMatcher;
    private final OpeningHours openingHours;

    // 장소·숙소 검색. 여행 도시 중심 50km 안, 최대 10개. 결과가 없으면 빈 목록 (FE가 주소 직접 입력으로 안내)
    @GetMapping("/api/places/search")
    public List<Candidate> search(@LoginUser Long userId, @RequestParam String query, @RequestParam Long tripId) {
        if (query.isBlank()) throw new BusinessException(ErrorCode.INVALID_INPUT, "query 값이 비어 있습니다.");
        // 구글이 좌표 검색어를 400으로 거절해서 502가 나던 것 → 우리 쪽 400 (이름 없는 핀 이름이 좌표라 FE가 그대로 넣을 수 있음)
        if (PlaceMatcher.isCoordinates(query)) throw new BusinessException(ErrorCode.INVALID_INPUT, "좌표로는 검색할 수 없습니다. 장소 이름을 입력해 주세요.");
        City city = memberTripCity(userId, tripId);
        return placeMatcher.searchNear(query, city.lat(), city.lng(), SEARCH_RADIUS_M);
    }

    // 장소 추가 시트 "지금 동선에서 가까운 순". FE가 정한 중심(지금 동선) 2km 안 인기 장소 최대 20개, 가까운 순
    // category를 빼면 추천 탭(모든 유형), 넣으면 그 탭(관광·맛집·카페·쇼핑 등). 장소 교체의 "더 가까운 곳"도 같은 API
    @GetMapping("/api/places/nearby")
    public List<Candidate> nearby(@LoginUser Long userId, @RequestParam Long tripId,
                                  @RequestParam double lat, @RequestParam double lng,
                                  @RequestParam(required = false) PlaceCategory category) {
        if (category == PlaceCategory.OTHER) throw new BusinessException(ErrorCode.INVALID_INPUT, "기타 유형으로는 추천할 수 없습니다.");
        City city = memberTripCity(userId, tripId);
        // 여행 도시 밖(잘못된 좌표, NaN 포함)은 구글을 부르지 않음. NaN 은 비교가 늘 false 라 <= 로 확인
        if (!(PlaceMatcher.meters(city.lat(), city.lng(), lat, lng) <= SEARCH_RADIUS_M)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "여행 도시 근처 좌표가 아닙니다.");
        }
        return placeMatcher.searchNearby(lat, lng, category);
    }

    // 장소 상세 영업시간. 시각과 "지금 영업 중"은 여행 도시 현지 시간. 영업시간 정보가 없는 장소(거리·동네 등)는 204
    // ponytail: 열 때마다 구글 Place Details(영업시간) 요금. 많이 열리면 장소별 캐시
    @GetMapping("/api/places/{placeId}/hours")
    public ResponseEntity<PlaceHoursResponse> hours(@LoginUser Long userId, @PathVariable String placeId,
                                                    @RequestParam Long tripId) {
        City city = memberTripCity(userId, tripId);
        Hours hours = openingHours.get(placeId);
        if (hours == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(PlaceHoursResponse.of(hours, ZonedDateTime.now(ZoneId.of(city.timezone()))));
    }

    // 없는 여행·구성원이 아니면 404, 지원하지 않는 도시면 400
    private City memberTripCity(Long userId, Long tripId) {
        Trip trip = tripService.findMemberTrip(userId, tripId);
        return CityController.findByName(trip.getCity())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_CITY));
    }
}
