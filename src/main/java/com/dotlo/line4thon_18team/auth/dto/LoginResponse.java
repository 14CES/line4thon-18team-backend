package com.dotlo.line4thon_18team.auth.dto;

/**
 * 로그인 응답.
 *
 * @param newUser 처음 가입한 사용자면 true (프론트에서 첫 화면 안내 등에 사용)
 */
public record LoginResponse(String accessToken, String refreshToken, boolean newUser) {
}
