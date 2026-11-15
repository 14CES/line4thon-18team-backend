package com.dotlo.line4thon_18team.chat.dto;

import com.dotlo.line4thon_18team.chat.poll.dto.PollView;

import java.util.List;

/**
 * @param messages 커서 이후 메시지, 오래된 것부터
 * @param polls    메시지 커서와 관계없이 내려주는 최근 투표 상태
 */
public record ChatMessageListResponse(List<ChatMessageResponse> messages, List<PollView> polls) {
}
