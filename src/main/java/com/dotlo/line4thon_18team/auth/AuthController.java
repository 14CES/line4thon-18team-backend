package com.dotlo.line4thon_18team.auth;

import com.dotlo.line4thon_18team.auth.dto.GoogleLoginRequest;
import com.dotlo.line4thon_18team.auth.dto.LoginResponse;
import com.dotlo.line4thon_18team.auth.dto.RefreshTokenRequest;
import com.dotlo.line4thon_18team.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // 구글 로그인 (처음이면 가입)
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.login(request.code()));
    }

    // access 토큰 만료 시 재발급 (refresh 토큰도 새로 바뀜)
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    // 로그아웃 → 204 No Content
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
