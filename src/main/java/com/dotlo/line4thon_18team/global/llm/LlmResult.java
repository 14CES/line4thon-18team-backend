package com.dotlo.line4thon_18team.global.llm;

/**
 * 검사를 통과한 LLM 결과.
 *
 * @param value    결과 객체
 * @param attempts 성공까지 걸린 시도 횟수 (1이면 첫 시도에 성공)
 */
public record LlmResult<O>(O value, int attempts) {
}
