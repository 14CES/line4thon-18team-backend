package com.dotlo.line4thon_18team.global.error;

import lombok.Getter;

/**
 * 우리가 예상한 에러(없는 여행, 잘못된 파일 등)를 던질 때 쓰는 예외.
 * GlobalExceptionHandler 가 받아서 ErrorCode 에 맞는 응답으로 바꿔준다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 기본 메시지 대신 더 구체적인 안내가 필요할 때
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
