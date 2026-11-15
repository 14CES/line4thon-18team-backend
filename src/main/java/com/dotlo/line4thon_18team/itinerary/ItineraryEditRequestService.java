package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.itinerary.edit.ItineraryDiff;
import com.dotlo.line4thon_18team.itinerary.edit.dto.ItineraryEditPreview;
import com.dotlo.line4thon_18team.itinerary.edit.entity.ItineraryEditRequest;
import com.dotlo.line4thon_18team.itinerary.edit.repository.ItineraryEditRequestRepository;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;
import com.dotlo.line4thon_18team.itinerary.version.entity.ItinerarySource;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

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
     * @throws BusinessException 없는 여행·구성원이 아닌 여행(404), 저장된 일정이 없음(404)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ItineraryEditPreview request(long tripId, long userId, String message) {
        ItineraryVersionView current = itineraryService.getLatest(tripId, userId);
        EditedItinerary edited = editService.edit(tripId, userId, current.draft(), message);
        ItineraryDiff diff = ItineraryDiff.between(current.draft(), edited.draft());

        long editRequestId = transactionTemplate.execute(status -> {
            Trip trip = tripService.findMemberTrip(userId, tripId);
            return editRequestRepository.save(ItineraryEditRequest.create(trip, current.version(), message,
                    edited.reason(), userId,
                    objectMapper.writeValueAsString(edited.draft()),
                    objectMapper.writeValueAsString(edited.validation()))).getId();
        });

        return new ItineraryEditPreview(editRequestId, current.version(), edited.reason(),
                new ItineraryEditPreview.Itinerary(edited.draft(), edited.validation()), diff);
    }

    /**
     * 보관한 수정안을 새 일정 버전으로 저장한다 (EDIT-APPLY-001).
     *
     * <p>보관해 둔 검증 결과를 그대로 저장한다. 여기서 다시 검증하면 영업시간이 그사이 바뀌어
     * 화면에 보여준 시각과 저장되는 시각이 달라지고, 외부 조회가 또 들어가 느려진다.
     *
     * <p>그사이 일정이 바뀌었는지는 {@link ItineraryService#save}의 기준 버전 검사가 그대로 잡는다.
     *
     * @param editRequestId 텍스트 수정 요청 응답에서 받은 본인의 수정안 ID
     * @throws BusinessException 접근할 수 없는 여행·수정안(404), 그사이 일정이 바뀜(409)
     */
    @Transactional
    public ItineraryVersionView apply(long tripId, long userId, long editRequestId) {
        tripService.findMemberTrip(userId, tripId);
        ItineraryEditRequest edit = editRequestRepository
                .findByIdAndTripIdAndCreatedBy(editRequestId, tripId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EDIT_REQUEST_NOT_FOUND));
        return saveEdit(tripId, userId, edit);
    }

    /** 구버전 클라이언트는 자기 수정안을 하나로 특정할 수 있을 때만 적용한다. */
    @Transactional
    public ItineraryVersionView applyByBaseVersion(long tripId, long userId, int baseVersion) {
        tripService.findMemberTrip(userId, tripId);
        List<ItineraryEditRequest> edits = editRequestRepository
                .findTop2ByTripIdAndBaseVersionAndCreatedBy(tripId, baseVersion, userId);
        if (edits.isEmpty()) {
            throw new BusinessException(ErrorCode.EDIT_REQUEST_NOT_FOUND);
        }
        if (edits.size() > 1) {
            throw new BusinessException(ErrorCode.EDIT_REQUEST_ID_REQUIRED);
        }
        return saveEdit(tripId, userId, edits.get(0));
    }

    private ItineraryVersionView saveEdit(long tripId, long userId, ItineraryEditRequest edit) {
        return itineraryService.save(tripId, userId, ItinerarySource.AI_EDITED, edit.getBaseVersion(),
                objectMapper.readValue(edit.getContent(), ItineraryDraft.class),
                objectMapper.readValue(edit.getValidation(), ValidationResult.class));
    }
}
