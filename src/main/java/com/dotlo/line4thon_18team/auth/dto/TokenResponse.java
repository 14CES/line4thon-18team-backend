package com.dotlo.line4thon_18team.auth.dto;

/**
 * 토큰 재발급 응답. refresh 토큰도 새로 바뀌므로 둘 다 다시 저장해야 한다.
 */
public record TokenResponse(String accessToken, String refreshToken) {
}
