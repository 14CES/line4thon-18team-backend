package com.dotlo.line4thon_18team.global.llm;

/**
 * LLM 제공자 연동 지점. 지금은 OpenAI 구현만 있다.
 *
 * <p>구현체는 한 번만 호출하고 재시도는 하지 않는다. 재시도·형식 검사는 {@link StructuredLlmExecutor}가 맡는다.
 */
public interface LlmClient {

    /**
     * @throws LlmCallException 네트워크·HTTP 오류. {@link LlmCallException#isRetryable()}이 true면 다시 시도할 만하다.
     */
    LlmResponse complete(LlmRequest request);
}
