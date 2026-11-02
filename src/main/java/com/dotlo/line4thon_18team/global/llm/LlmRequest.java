package com.dotlo.line4thon_18team.global.llm;

import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

/**
 * 모델 호출 한 번에 필요한 값.
 *
 * @param taskName 작업 이름. 로그와 OpenAI json_schema 이름으로 쓴다.
 * @param messages 대화 (system, user, 재시도 시 assistant/user가 뒤에 붙는다)
 * @param schema   응답이 따라야 할 JSON 스키마 (OpenAI strict 모드로 강제)
 */
public record LlmRequest(String taskName, List<LlmMessage> messages, JsonNode schema) {

    public LlmRequest {
        Objects.requireNonNull(taskName, "taskName");
        Objects.requireNonNull(schema, "schema");
        messages = List.copyOf(messages);
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages가 비어 있습니다");
        }
    }
}
