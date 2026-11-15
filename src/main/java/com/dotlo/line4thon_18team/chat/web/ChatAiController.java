package com.dotlo.line4thon_18team.chat.web;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.chat.ChatCoordinationService;
import com.dotlo.line4thon_18team.chat.dto.CoordinationCommand;
import com.dotlo.line4thon_18team.chat.dto.CoordinationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅방 AI API (BE3). 로그인이 필요하고 여행 구성원이 아니면 404다.
 * 메시지 저장·조회는 채팅 쪽 컨트롤러가 맡는다.
 */
@RestController
@RequestMapping("/api/trips/{tripId}/chat")
@RequiredArgsConstructor
public class ChatAiController {

    private final ChatCoordinationService coordinationService;

    /**
     * 채팅방 대화를 읽어 사람별 의견을 정리한 뒤 조율 방향을 제안하고 AI 메시지로 남긴다.
     *
     * <p>다른 구성원은 메시지 폴링으로 조율 카드와 방향 선택지를 받는다.
     * 고른 방향의 label 을 사람 메시지로 보내면, 거기서 투표 후보 만들기로 이어진다.
     *
     * <p>AI 호출이 들어가서 응답이 몇 초에서 수십 초 걸릴 수 있다.
     */
    @PostMapping("/coordinate")
    public CoordinationResponse coordinate(@PathVariable("tripId") long tripId, @LoginUser Long userId,
                                           @Valid @RequestBody CoordinationCommand command) {
        return coordinationService.coordinate(tripId, userId, command.messages());
    }
}
