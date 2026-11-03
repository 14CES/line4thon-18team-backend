package com.dotlo.line4thon_18team.global.llm;

import java.util.Objects;

/** LLM에 보내는 대화 메시지 한 건. */
public record LlmMessage(Role role, String content) {

    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant");

        private final String apiValue;

        Role(String apiValue) {
            this.apiValue = apiValue;
        }

        public String apiValue() {
            return apiValue;
        }
    }

    public LlmMessage {
        Objects.requireNonNull(role, "role");
        content = content == null ? "" : content;
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content);
    }
}
