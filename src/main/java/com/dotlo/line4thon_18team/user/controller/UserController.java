package com.dotlo.line4thon_18team.user.controller;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.user.dto.MyStatsResponse;
import com.dotlo.line4thon_18team.user.dto.NicknameCheckResponse;
import com.dotlo.line4thon_18team.user.dto.ProfileUpdateRequest;
import com.dotlo.line4thon_18team.user.dto.SignupRequest;
import com.dotlo.line4thon_18team.user.dto.UserResponse;
import com.dotlo.line4thon_18team.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    // 가입 마무리 (약관 동의 + 닉네임 + 캐릭터). 구글 로그인 응답의 signupCompleted 가 false 면 부른다
    @PostMapping("/me/signup")
    public ResponseEntity<UserResponse> signup(@LoginUser Long userId, @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(userService.signup(userId, request));
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
