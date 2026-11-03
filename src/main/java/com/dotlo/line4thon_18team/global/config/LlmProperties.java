package com.dotlo.line4thon_18team.global.config;

import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.global.llm.openai.OpenAiSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** application.yaml의 dotlo.llm.* 설정 (API 키는 .env의 OPENAI_API_KEY). */
@ConfigurationProperties(prefix = "dotlo.llm")
public record LlmProperties(Integer maxAttempts, OpenAi openai) {

    public LlmProperties {
        if (openai == null) {
            openai = new OpenAi(null, null, null, null, null, null);
        }
    }

    public int maxAttemptsOrDefault() {
        return maxAttempts == null || maxAttempts < 1 ? StructuredLlmExecutor.DEFAULT_MAX_ATTEMPTS : maxAttempts;
    }

    public record OpenAi(String apiKey, String baseUrl, String model, String reasoningEffort,
                         Duration connectTimeout, Duration requestTimeout) {

        public OpenAiSettings toSettings() {
            return new OpenAiSettings(apiKey, baseUrl, model, reasoningEffort, connectTimeout, requestTimeout);
        }

        /** 설정 내용이 로그에 찍혀도 키가 보이지 않도록 가린다. */
        @Override
        public String toString() {
            return toSettings().toString();
        }
    }
}
