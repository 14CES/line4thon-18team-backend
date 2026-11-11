package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.itinerary.edit.ItineraryDiff;
import com.dotlo.line4thon_18team.itinerary.edit.dto.ItineraryEditPreview;
import com.dotlo.line4thon_18team.itinerary.edit.entity.ItineraryEditRequest;
import com.dotlo.line4thon_18team.itinerary.edit.repository.ItineraryEditRequestRepository;
import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * 텍스트 수정 요청 (EDIT-TEXT-001). 저장된 최신 일정을 읽어 AI에게 고치게 하고, 변경점을 계산해
 * 수정안으로 보관한다. 일정으로 저장하는 것은 사용자가 적용을 누를 때(EDIT-APPLY-001) 한다.
 *
 * <p>AI 수정 자체는 {@link ItineraryEditService}가 하고, 이 클래스는 앞뒤(현재 일정 읽기,
 * 변경점 계산, 수정안 보관)를 맡는다. 수정은 생성과 달리 "보관한 뒤 나중에 적용"하는 두 단계라서
 * 보관 테이블까지 끼고, 한 클래스에 넣으면 AI 호출과 저장 트랜잭션이 섞인다.
 */
@Service
@RequiredArgsConstructor
public class ItineraryEditRequestService {

    private final ItineraryService itineraryService;
    private final ItineraryEditService editService;
    private final ItineraryEditRequestRepository editRequestRepository;
    private final TripService tripService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /**
     * <p>이 메서드에는 트랜잭션을 걸지 않는다. AI 응답과 이동시간 조회를 기다리는 동안 DB 연결을
     * 쥐고 있지 않기 위해서다. 현재 일정 조회와 수정안 저장만 각자 트랜잭션에서 한다.
     *
     * @throws BusinessException 없는 여행·남의 여행(404), 저장된 일정이 없음(404)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ItineraryEditPreview request(long tripId, long userId, String message) {
        ItineraryVersionView current = itineraryService.getLatest(tripId, userId);
        EditedItinerary edited = editService.edit(tripId, userId, current.draft(), message);
        ItineraryDiff diff = ItineraryDiff.between(current.draft(), edited.draft());

        transactionTemplate.executeWithoutResult(status -> {
            Trip trip = tripService.findOwnedTrip(userId, tripId);
            editRequestRepository.save(ItineraryEditRequest.create(trip, current.version(), message,
                    edited.reason(), userId,
                    objectMapper.writeValueAsString(edited.draft()),
                    objectMapper.writeValueAsString(edited.validation())));
        });

        return new ItineraryEditPreview(current.version(), edited.reason(),
                new ItineraryEditPreview.Itinerary(edited.draft(), edited.validation()), diff);
    }
}
