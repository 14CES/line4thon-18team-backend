package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.city.CityController;
import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.itinerary.validation.ItineraryValidator;
import com.dotlo.line4thon_18team.itinerary.validation.ValidationPolicy;
import com.dotlo.line4thon_18team.itinerary.validation.model.GeoPoint;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.TripContext;
import com.dotlo.line4thon_18team.itinerary.validation.model.ValidationOrigin;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.place.OpeningHours;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.preference.PreferenceService;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

/**
 * 일정 검증 (GEN-CHECK-001). 방문 순서만 받아서 시각을 계산하고 영업시간·이동시간 문제를 찾는다.
 *
 * <p>일정 생성·수정(BE3)뿐 아니라 직접 편집 저장(BE1), 인기 코스 담기(BE2)에서도 이 함수를 부르면 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItineraryValidationService {

    private final TripService tripService;
    private final SavedPlaceRepository savedPlaceRepository;
    private final PreferenceService preferenceService;
    private final OpeningHours openingHours;          // BE2 영업시간
    private final TravelTimeProvider travelTimeProvider;
    private final ValidationPolicy validationPolicy;

    /**
     * @param origin AI가 만든 일정이면 AI_GENERATED(일정 충돌을 오류로), 사용자가 직접 고친 일정이면 USER_EDITED(경고로)
     * @throws BusinessException 남의 여행·없는 여행(404), 일정 입력이 잘못된 경우(400)
     */
    public ValidationResult validate(long tripId, long userId, ItineraryDraft draft, ValidationOrigin origin) {
        Trip trip = tripService.findOwnedTrip(userId, tripId);
        List<SavedPlace> places = savedPlaceRepository.findAllByTripIdOrderByIdAsc(tripId);

        ItineraryValidator validator = new ItineraryValidator(
                new SavedPlaceInfoProvider(places, openingHours::get), travelTimeProvider, validationPolicy);
        try {
            return validator.validate(draft, tripContext(trip), preferenceService.profileOrDefault(tripId, userId),
                    origin);
        } catch (IllegalArgumentException e) {
            // 여행 기간 밖 날짜, 같은 날짜 중복, itemId 중복
            throw new BusinessException(ErrorCode.INVALID_ITINERARY,
                    ErrorCode.INVALID_ITINERARY.getMessage() + " " + e.getMessage());
        }
    }

    /*
     * 숙소·항공편 입력(TRIP-STAY-001)이 아직 없어서, 지금은 도시 중심을 숙소로 두고 계산한다.
     * 숙소가 생기면 hotel 좌표만 바꾸면 되고, 항공 도착·출발 시각을 넣으면 첫날·마지막 날이 자동으로 줄어든다.
     */
    private TripContext tripContext(Trip trip) {
        CityController.City city = CityController.findByName(trip.getCity())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_CITY));
        GeoPoint cityCenter = new GeoPoint(city.lat(), city.lng());
        return new TripContext(ZoneId.of(city.timezone()), trip.getStartDate(), trip.getEndDate(),
                cityCenter, null, null, null);
    }
}
