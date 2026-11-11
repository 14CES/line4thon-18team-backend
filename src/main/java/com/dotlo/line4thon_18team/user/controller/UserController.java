package com.dotlo.line4thon_18team.user.controller;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.user.dto.MyStatsResponse;
import com.dotlo.line4thon_18team.user.dto.NicknameCheckResponse;
import com.dotlo.line4thon_18team.user.dto.ProfileUpdateRequest;
import com.dotlo.line4thon_18team.user.dto.UserResponse;
import com.dotlo.line4thon_18team.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // 프로필 수정 (닉네임, 한 줄 소개, 캐릭터)
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@LoginUser Long userId,
                                                      @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    // 닉네임 중복 확인. 형식이 틀리면 400
    @GetMapping("/nickname/check")
    public ResponseEntity<NicknameCheckResponse> checkNickname(@LoginUser Long userId,
                                                               @RequestParam String nickname) {
        return ResponseEntity.ok(userService.checkNickname(userId, nickname));
    }

    // 마이 홈 숫자 (저장한 장소, 저장한 코스, 다녀온 여행)
    @GetMapping("/me/stats")
    public ResponseEntity<MyStatsResponse> getMyStats(@LoginUser Long userId) {
        return ResponseEntity.ok(userService.getMyStats(userId));
    }
}
