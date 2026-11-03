package com.dotlo.line4thon_18team.global.llm;

/**
 * 모델 응답 한 건.
 *
 * @param content      모델이 만든 JSON 문자열 (거부했으면 null일 수 있음)
 * @param refusal      모델이 안전 정책으로 거부한 경우의 설명
 * @param finishReason "stop", "length"(잘림), "content_filter" 등
 */
public record LlmResponse(String content, String refusal, String finishReason) {

    public boolean isRefusal() {
        return refusal != null && !refusal.isBlank();
    }

    public boolean isTruncated() {
        return "length".equals(finishReason);
    }
}
