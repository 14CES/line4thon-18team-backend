package com.dotlo.line4thon_18team.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 컨트롤러에서 터진 예외를 전부 받아서 ErrorResponse 모양으로 통일해 내려준다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 우리가 직접 던진 예외 (없는 여행, 잘못된 파일 등)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return toResponse(e.getErrorCode(), e.getMessage());
    }

    // @Valid 검증 실패 (필수값 누락 등) -> 첫 번째 에러 메시지를 안내
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = (fieldError != null)
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : ErrorCode.INVALID_INPUT.getMessage();
        return toResponse(ErrorCode.INVALID_INPUT, message);
    }

    // JSON 문법 오류, 본문 없음, 날짜 형식 오류 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return toResponse(ErrorCode.INVALID_REQUEST_BODY, ErrorCode.INVALID_REQUEST_BODY.getMessage());
    }

    // /api/trips/abc 처럼 숫자 자리에 글자가 들어온 경우
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return toResponse(ErrorCode.INVALID_INPUT, e.getName() + " 값이 올바르지 않습니다.");
    }

    // multipart 요청에 'file' 이 빠진 경우
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e) {
        return toResponse(ErrorCode.FILE_REQUIRED, ErrorCode.FILE_REQUIRED.getMessage());
    }

    // 업로드 용량 초과 (MultipartException 보다 먼저 잡혀야 해서 따로 둔다)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return toResponse(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.getMessage());
    }

    // multipart 가 아닌 요청으로 업로드 API를 호출한 경우
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException e) {
        return toResponse(ErrorCode.FILE_REQUIRED, ErrorCode.FILE_REQUIRED.getMessage());
    }

    // JSON 을 받는 API 에 text/plain 등 다른 형식으로 보낸 경우
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return toResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    // 위에서 못 잡은 나머지 -> 500. 원인 파악을 위해 로그는 꼭 남긴다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return toResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, message));
    }
}
