package com.dotlo.line4thon_18team.chat;

import com.dotlo.line4thon_18team.chat.ai.CoordinationInput;
import com.dotlo.line4thon_18team.chat.ai.CoordinationOutput;
import com.dotlo.line4thon_18team.chat.ai.CoordinationTask;
import com.dotlo.line4thon_18team.chat.dto.CoordinationResponse;
import com.dotlo.line4thon_18team.chat.entity.ChatMessage;
import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 의견 조율 (COLLAB-COORD-001). 채팅방 대화를 읽고 사람별로 무엇을 원하는지 정리한 뒤,
 * 어떻게 조율할지 방향을 제안한다. 일정이나 장소는 건드리지 않는다.
 *
 * <p>AI 에는 사람을 번호로 넘기고 번호로 돌려받아 서버가 사용자 id 로 되돌린다. 화면에서는 요약이
 * 이름과 함께 나와서, 엉뚱한 사람 말이 붙으면 바로 티가 난다.
 *
 * <p>트랜잭션을 걸지 않는다. AI 응답을 기다리는 동안 DB 연결을 쥐고 있지 않기 위해서다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ChatCoordinationService {

    static final String OPTIONS_TITLE = "어떤 방향으로 조율해 볼까요?";
    static final String TALK_MORE_ID = "talk-more";
    static final String TALK_MORE_LABEL = "조금 더 이야기해 볼게요";

    private final StructuredLlmExecutor llm;
    private final CoordinationTask coordinationTask;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

    /**
     * @throws BusinessException 구성원이 아닌 여행·없는 여행(404), 정리할 대화가 하나도 남지 않음(400)
     */
    public CoordinationResponse coordinate(long tripId, long userId) {
        ChatMessageService.CoordinationHistory history = chatMessageService.loadCoordinationHistory(userId, tripId);
        if (history.messages().isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MESSAGES_TO_COORDINATE);
        }

        Map<Long, Integer> numberByUser = new LinkedHashMap<>();
        List<CoordinationInput.Line> lines = new ArrayList<>();
        for (ChatMessage message : history.messages()) {
            int number = numberByUser.computeIfAbsent(message.getSender().getId(),
                    senderId -> numberByUser.size() + 1);
            lines.add(new CoordinationInput.Line(number, message.getText()));
        }
        CoordinationOutput output = llm.execute(coordinationTask,
                new CoordinationInput(history.city(), lines)).value();

        CoordinationResponse response = new CoordinationResponse(output.text(),
                new CoordinationResponse.Card(CoordinationResponse.OPINION_SUMMARY,
                        summaries(output, numberByUser)),
                options(output));
        String payload = objectMapper.writeValueAsString(Map.of("card", response.card(), "options", response.options()));
        chatMessageService.appendAiForMember(tripId, userId, response.text(), payload);
        return response;
    }

    /** 번호를 사용자 id 로 되돌린다. 모르는 번호는 CoordinationTask 가 걸러서 여기까지 오지 않는다. */
    private static List<CoordinationResponse.Summary> summaries(CoordinationOutput output,
                                                                Map<Long, Integer> numberByUser) {
        Map<Integer, Long> userByNumber = new LinkedHashMap<>();
        numberByUser.forEach((user, number) -> userByNumber.put(number, user));
        List<CoordinationResponse.Summary> summaries = new ArrayList<>();
        for (CoordinationOutput.Summary summary : output.summaries()) {
            Long user = userByNumber.get(summary.memberNumber());
            if (user != null) {
                summaries.add(new CoordinationResponse.Summary(user, summary.point()));
            }
        }
        return List.copyOf(summaries);
    }

    /** AI 가 낸 방향 뒤에 "조금 더 이야기해 볼게요"를 붙인다. 아직 정하지 않을 자유는 서버가 보장한다. */
    private static CoordinationResponse.Options options(CoordinationOutput output) {
        List<CoordinationResponse.Item> items = new ArrayList<>();
        for (int i = 0; i < output.directions().size(); i++) {
            items.add(new CoordinationResponse.Item("d" + (i + 1), output.directions().get(i)));
        }
        items.add(new CoordinationResponse.Item(TALK_MORE_ID, TALK_MORE_LABEL));
        return new CoordinationResponse.Options(OPTIONS_TITLE, List.copyOf(items));
    }
}
