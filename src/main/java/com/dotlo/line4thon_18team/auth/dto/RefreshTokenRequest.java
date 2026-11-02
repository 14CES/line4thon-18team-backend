package com.dotlo.line4thon_18team.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급·로그아웃 요청.
 * 예) { "refreshToken": "eyJ..." }
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refresh 토큰이 필요합니다.")
        String refreshToken
) {
}
