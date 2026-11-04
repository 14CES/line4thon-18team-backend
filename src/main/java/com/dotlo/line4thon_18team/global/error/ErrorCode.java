package com.dotlo.line4thon_18team.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 서버에서 내려주는 모든 에러의 목록.
 * 새 에러가 필요하면 자기 도메인 구역에 한 줄 추가하고 BusinessException 으로 던지면 된다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 공통 =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문(JSON) 형식이 올바르지 않습니다."),
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다. 'file' 이름으로 파일을 첨부해 주세요."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일 크기가 너무 큽니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다. JSON 으로 보내 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 서비스 응답에 실패했습니다. 잠시 후 다시 시도해 주세요."),

    // ===== 계정 (BE1) =====
    GOOGLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "구글 로그인에 실패했습니다. 다시 시도해 주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다. 다시 로그인해 주세요."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 토큰을 재발급해 주세요."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // ===== 여행 (BE1) =====
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "종료일이 시작일보다 빠를 수 없습니다."),
    INVALID_COMPANION_COUNT(HttpStatus.BAD_REQUEST, "동행 인원이 올바르지 않습니다. 친구·가족은 2~10명이에요."),
    UNSUPPORTED_CITY(HttpStatus.BAD_REQUEST, "지원하지 않는 도시입니다."),

    // ===== 장소 (BE1 / BE2) =====
    CITY_NOT_FOUND(HttpStatus.NOT_FOUND, "도시를 찾을 수 없습니다."),

    // ===== 일정·AI (BE3) =====
    PREFERENCE_TEXT_TOO_LONG(HttpStatus.BAD_REQUEST, "취향은 500자 이하로 입력해 주세요."),
    INVALID_PREFERENCE(HttpStatus.BAD_REQUEST, "취향 값이 올바르지 않습니다."),
    INVALID_ITINERARY(HttpStatus.BAD_REQUEST, "일정 정보가 올바르지 않습니다."),

    ; // 에러 추가는 이 위에 (각 줄은 쉼표로 끝내기)

    private final HttpStatus status;
    private final String message;
}
