package com.dotlo.line4thon_18team.global.llm;

import java.util.List;

/** 네트워크·HTTP 단계의 실패 (타임아웃, 429, 5xx, 잘못된 요청, API 키 없음 등). */
public class LlmCallException extends LlmException {

    private final boolean retryable;
    private final Integer httpStatus;

    public LlmCallException(String taskName, String message, boolean retryable, Integer httpStatus, Throwable cause) {
        this(taskName, message, retryable, httpStatus, cause, List.of());
    }

    public LlmCallException(String taskName, String message, boolean retryable, Integer httpStatus,
                            Throwable cause, List<String> attemptErrors) {
        super(taskName, message, attemptErrors, cause);
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    /** 잠시 후 다시 시도하면 성공할 수 있는 오류인지 (타임아웃, 429, 5xx). */
    public boolean isRetryable() {
        return retryable;
    }

    /** HTTP 상태 코드. 연결 자체가 실패했으면 null. */
    public Integer getHttpStatus() {
        return httpStatus;
    }
}
