package com.dotlo.line4thon_18team.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 구글 로그인 요청.
 * 예) { "code": "4/0AXXXX..." }  ← 구글 로그인 후 받은 인증 코드
 */
public record GoogleLoginRequest(
        @NotBlank(message = "구글 인증 코드가 필요합니다.")
        String code
) {
}
