package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.itinerary.edit.ItineraryDiff;
import com.dotlo.line4thon_18team.itinerary.edit.dto.ItineraryPlaceAddResult;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;
import com.dotlo.line4thon_18team.itinerary.version.entity.ItinerarySource;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.place.service.PlaceEnrichService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 하나를 일정에 넣기 (COLLAB-ADD-001). 투표에서 1위를 한 장소나 사용자가 고른 장소를
 * AI가 알맞은 날과 순서에 넣고 새 버전으로 저장한다.
 *
 * <p>AI 호출은 텍스트 수정({@link ItineraryEditService})을 그대로 쓴다. 수정 쪽이 이미
 * "지금 일정 + 넣을 수 있는 장소 목록"을 넘기고 자리를 정하게 되어 있어서, 요청 문장만 서버가 만든다.
 *
 * <p>텍스트 수정과 달리 미리보기를 거치지 않고 바로 저장한다. 장소 하나를 넣는 것은 변경 범위가 좁고,
 * 마음에 들지 않으면 되돌리기({@code /{version}/revert})로 한 번에 되돌릴 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ItineraryPlaceAddService {

    private final PlaceEnrichService placeEnrichService;
    private final SavedPlaceRepository savedPlaceRepository;
    private final ItineraryService itineraryService;
    private final ItineraryEditService editService;

    /**
     * @throws BusinessException 구성원이 아닌 여행·없는 여행(404), 없는 placeId(400),
     *                           저장된 일정이 없음(404), 폐업·도시 밖 장소(400),
     *                           이미 일정에 있는 장소(400), 넣을 자리를 못 찾음(400)
     */
    public ItineraryPlaceAddResult add(long tripId, long userId, String placeId, String note) {
        // 저장 장소에 없으면 BE2가 구글에서 받아 담는다. 이미 있으면 리스트 이름만 붙고 구글을 부르지 않는다.
        // 트랜잭션 밖에서 불러야 한다. add 는 여행 행을 잠근 채 구글 Details 를 부르기 때문에,
        // 바깥에 트랜잭션이 있으면 구글을 기다리는 동안 같은 여행의 장소 추가·보강이 모두 줄을 선다
        placeEnrichService.add(userId, tripId, placeId);

        SavedPlace place = savedPlaceRepository.findFirstByTripIdAndPlaceId(tripId, placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PLACE_ID));
        if (place.getMatchStatus() != PlaceMatchStatus.MATCHED) {
            // 사용자가 지목한 장소라서 조용히 빼지 않고 왜 못 넣는지 알려준다
            throw new BusinessException(ErrorCode.PLACE_NOT_PLANNABLE,
                    ErrorCode.PLACE_NOT_PLANNABLE.getMessage() + " " + describe(place.getMatchStatus()));
        }

        ItineraryVersionView current = itineraryService.getLatest(tripId, userId);
        if (contains(current.draft(), placeId)) {
            throw new BusinessException(ErrorCode.PLACE_ALREADY_IN_ITINERARY);
        }

        EditedItinerary edited = editService.edit(tripId, userId, current.draft(), request(place.getName(), note));
        if (!contains(edited.draft(), placeId)) {
            // AI가 넣을 자리를 못 찾은 경우(영업시간, 하루 개수). 똑같은 일정을 새 버전으로 쌓지 않는다
            throw new BusinessException(ErrorCode.PLACE_ADD_FAILED,
                    ErrorCode.PLACE_ADD_FAILED.getMessage() + " " + edited.reason());
        }

        ItineraryDiff diff = ItineraryDiff.between(current.draft(), edited.draft());
        ItineraryVersionView saved = itineraryService.save(tripId, userId, ItinerarySource.AI_EDITED,
                current.version(), edited.draft(), edited.validation());
        return new ItineraryPlaceAddResult(place.getName(), edited.reason(), saved, diff);
    }

    /** 장소 이름과 사용자 요청을 수정 요청 문장으로 만든다. 태그 제거는 PlanEditTask 가 한다. */
    private static String request(String name, String note) {
        StringBuilder request = new StringBuilder("일정에 '").append(name)
                .append("' 한 곳을 알맞은 날과 순서에 넣어 주세요. 다른 방문은 그대로 둡니다.");
        if (note != null && !note.isBlank()) {
            request.append(' ').append(note.strip());
        }
        return request.toString();
    }

    private static String describe(PlaceMatchStatus status) {
        return switch (status) {
            case CLOSED -> "폐업한 장소입니다.";
            case OUT_OF_CITY -> "여행 도시에서 50km 넘게 떨어진 장소입니다.";
            default -> "아직 확인이 끝나지 않은 장소입니다. 저장 장소 목록에서 먼저 확인해 주세요.";
        };
    }

    private static boolean contains(ItineraryDraft draft, String placeId) {
        return draft.days().stream()
                .flatMap(day -> day.visits().stream())
                .anyMatch(visit -> placeId.equals(visit.placeId()));
    }
}
