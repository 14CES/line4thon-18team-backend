package com.dotlo.line4thon_18team.trip.service;

import com.dotlo.line4thon_18team.city.CityController;
import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.trip.dto.TripCreateRequest;
import com.dotlo.line4thon_18team.trip.dto.TripResponse;
import com.dotlo.line4thon_18team.trip.dto.TripUpdateRequest;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.entity.TripStatus;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public TripResponse createTrip(Long userId, TripCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateSupportedCity(request.city());

        Trip trip = Trip.create(user, request.city(), request.startDate(), request.endDate(),
                request.companion(), request.companionCount());
        return TripResponse.from(tripRepository.save(trip));
    }

    public List<TripResponse> getMyTrips(Long userId) {
        return tripRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(TripResponse::from)
                .toList();
    }

    // 홈의 '이어서 만들기': 가장 최근에 손댄 작성 중 여행 (없으면 비어 있음)
    public Optional<TripResponse> getInProgressTrip(Long userId) {
        return tripRepository.findFirstByUserIdAndStatusOrderByUpdatedAtDesc(userId, TripStatus.DRAFT)
                .map(TripResponse::from);
    }

    public TripResponse getTrip(Long userId, Long tripId) {
        return TripResponse.from(findOwnedTrip(userId, tripId));
    }

    @Transactional
    public TripResponse updateTrip(Long userId, Long tripId, TripUpdateRequest request) {
        Trip trip = findOwnedTrip(userId, tripId);
        if (request.city() != null) {
            validateSupportedCity(request.city());
        }
        trip.update(request.city(), request.startDate(), request.endDate(),
                request.companion(), request.companionCount());
        return TripResponse.from(trip);
    }

    /**
     * 내 여행을 찾고, 없거나 남의 여행이면 404 를 던진다.
     * 장소·취향·일정 등 여행에 붙는 기능에서도 "이 사용자의 여행인지" 확인할 때 이 메서드를 쓴다.
     */
    public Trip findOwnedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    // 지원 도시 목록(GET /api/cities)에 있는 도시만 허용
    private void validateSupportedCity(String city) {
        if (CityController.findByName(city).isEmpty()) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_CITY);
        }
    }
}
