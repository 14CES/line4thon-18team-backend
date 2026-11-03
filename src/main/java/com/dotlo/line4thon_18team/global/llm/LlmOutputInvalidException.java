package com.dotlo.line4thon_18team.global.llm;

import java.util.List;

/** 최대 시도 횟수 동안 모델 출력이 한 번도 형식·의미 검사를 통과하지 못한 경우 (기능 상세: 3회 실패 → 오류 안내). */
public class LlmOutputInvalidException extends LlmException {

    public LlmOutputInvalidException(String taskName, List<String> attemptErrors) {
        super(taskName, "모델 출력이 " + attemptErrors.size() + "번 모두 검사를 통과하지 못했습니다", attemptErrors, null);
    }
}
