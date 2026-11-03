package com.dotlo.line4thon_18team.global.llm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** 정해 둔 순서대로 응답하거나 예외를 던지는 테스트용 LLM. 받은 요청을 기록한다. */
public class FakeLlmClient implements LlmClient {

    private final Deque<Object> script = new ArrayDeque<>();
    private final List<LlmRequest> requests = new ArrayList<>();

    public FakeLlmClient thenReturn(String content) {
        script.add(new LlmResponse(content, null, "stop"));
        return this;
    }

    public FakeLlmClient thenRespond(LlmResponse response) {
        script.add(response);
        return this;
    }

    public FakeLlmClient thenThrow(LlmCallException exception) {
        script.add(exception);
        return this;
    }

    public List<LlmRequest> requests() {
        return requests;
    }

    public int callCount() {
        return requests.size();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        requests.add(request);
        Object next = script.poll();
        if (next == null) {
            throw new AssertionError("예상보다 LLM 호출이 많습니다 (" + requests.size() + "번째)");
        }
        if (next instanceof LlmCallException exception) {
            throw exception;
        }
        return (LlmResponse) next;
    }
}
