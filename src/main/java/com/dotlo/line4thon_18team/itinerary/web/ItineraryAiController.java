package com.dotlo.line4thon_18team.itinerary.web;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.itinerary.GeneratedItineraryVersion;
import com.dotlo.line4thon_18team.itinerary.ItineraryGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 일정 API (BE3). 로그인이 필요하고 남의 여행이면 404다.
 *
 * <p>말로 수정(edit-requests)과 수정 적용(apply)도 여기에 붙인다. 저장·조회(BE1)는
 * {@code itinerary.version} 쪽 컨트롤러가 맡는다.
 */
@RestController
@RequestMapping("/api/trips/{tripId}/itineraries")
@RequiredArgsConstructor
public class ItineraryAiController {

    private final ItineraryGenerationService generationService;

    /**
     * 저장한 장소로 일정을 만들어 새 버전으로 저장한다.
     *
     * <p>AI 호출과 이동시간 조회가 들어가서 응답이 수십 초 걸릴 수 있다.
     * 장소가 하나도 매칭되지 않았으면 400, 숙소를 일부 밤만 넣었으면 400이다.
     */
    @PostMapping("/generate")
    public GeneratedItineraryVersion generate(@PathVariable("tripId") long tripId, @LoginUser Long userId) {
        return generationService.generateAndSave(tripId, userId);
    }
}
