package com.dotlo.line4thon_18team.user.controller;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.user.dto.UserResponse;
import com.dotlo.line4thon_18team.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 내 정보 (로그인 필요)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@LoginUser Long userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }
}
