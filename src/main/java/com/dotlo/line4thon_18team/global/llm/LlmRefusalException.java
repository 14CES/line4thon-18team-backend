package com.dotlo.line4thon_18team.global.llm;

import java.util.List;

/** 모델이 안전 정책 등으로 응답을 거부한 경우. 같은 입력으로 다시 물어도 결과가 같을 가능성이 높아 재시도하지 않는다. */
public class LlmRefusalException extends LlmException {

    private final String refusal;

    public LlmRefusalException(String taskName, String refusal, List<String> attemptErrors) {
        super(taskName, "모델이 응답을 거부했습니다: " + refusal, attemptErrors, null);
        this.refusal = refusal;
    }

    public String getRefusal() {
        return refusal;
    }
}
