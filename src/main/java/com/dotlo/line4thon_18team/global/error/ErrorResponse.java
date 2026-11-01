package com.dotlo.line4thon_18team.global.error;

/**
 * 모든 에러 응답의 모양.
 * 예) { "status": 404, "code": "TRIP_NOT_FOUND", "message": "여행을 찾을 수 없습니다." }
 */
public record ErrorResponse(int status, String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), message);
    }
}
