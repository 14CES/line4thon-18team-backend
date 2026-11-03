package com.dotlo.line4thon_18team.global.llm;

import java.util.List;

/** LLM 작업 실패의 공통 부모. 시도별 실패 내용을 같이 담는다 (로그·디버깅용). */
public class LlmException extends RuntimeException {

    private final String taskName;
    private final List<String> attemptErrors;

    protected LlmException(String taskName, String message, List<String> attemptErrors, Throwable cause) {
        super(message, cause);
        this.taskName = taskName;
        this.attemptErrors = attemptErrors == null ? List.of() : List.copyOf(attemptErrors);
    }

    public String getTaskName() {
        return taskName;
    }

    public List<String> getAttemptErrors() {
        return attemptErrors;
    }
}
