package com.dotlo.line4thon_18team.global.llm.openai;

import java.time.Duration;

/**
 * OpenAI 연동 설정. 빈 값은 기본값으로 채운다.
 *
 * @param apiKey          OPENAI_API_KEY. 비어 있으면 호출하지 않고 바로 실패한다 (취향 해석은 기본값으로 진행).
 * @param baseUrl         기본 https://api.openai.com/v1
 * @param model           Structured Outputs(json_schema)를 지원하는 모델
 * @param reasoningEffort 추론 모델에서만 쓰는 값 (minimal/low/medium/high). null이면 보내지 않는다.
 */
public record OpenAiSettings(String apiKey, String baseUrl, String model, String reasoningEffort,
                             Duration connectTimeout, Duration requestTimeout) {

    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_MODEL = "gpt-5-mini";

    public OpenAiSettings {
        apiKey = apiKey == null ? "" : apiKey.strip();
        baseUrl = isBlank(baseUrl) ? DEFAULT_BASE_URL : stripTrailingSlash(baseUrl.strip());
        model = isBlank(model) ? DEFAULT_MODEL : model.strip();
        reasoningEffort = isBlank(reasoningEffort) ? null : reasoningEffort.strip();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(60) : requestTimeout;
    }

    public boolean hasApiKey() {
        return !apiKey.isEmpty();
    }

    /** 로그에 키가 찍히지 않도록 가린다. */
    @Override
    public String toString() {
        return "OpenAiSettings[apiKey=" + (hasApiKey() ? "****" : "(없음)") + ", baseUrl=" + baseUrl
                + ", model=" + model + ", reasoningEffort=" + reasoningEffort
                + ", connectTimeout=" + connectTimeout + ", requestTimeout=" + requestTimeout + "]";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
