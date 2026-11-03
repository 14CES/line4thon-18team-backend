package com.dotlo.line4thon_18team.preference.web;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.preference.InterpretationResult;
import com.dotlo.line4thon_18team.preference.PreferenceInput;
import com.dotlo.line4thon_18team.preference.PreferenceService;
import com.dotlo.line4thon_18team.preference.PreferenceView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 취향 API (API 명세서 '취향' 파트, BE3). 모두 로그인이 필요하다 ({@link LoginUser}).
 *
 * <p>로컬에서는 AUTH_DEV_HEADER_ENABLED=true 일 때 토큰 대신 X-User-Id 헤더로도 호출할 수 있다.
 * TODO 여행 구성원인지 권한 확인은 여행 API(BE1)가 생기면 추가한다.
 */
@RestController
@RequestMapping("/api/trips/{tripId}/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    /** 텍스트 취향 AI 해석. 해석 결과를 칩으로 반환하고 저장하지 않는다. */
    @PostMapping("/analyze")
    public InterpretationResult analyze(@PathVariable("tripId") long tripId,
                                        @LoginUser Long userId,
                                        @RequestBody InterpretRequest request) {
        return preferenceService.interpret(tripId, userId, request.text());
    }

    /** 취향 칩 저장 (전체 덮어쓰기). */
    @PutMapping
    public PreferenceView save(@PathVariable("tripId") long tripId,
                               @LoginUser Long userId,
                               @RequestBody PreferenceInput input) {
        return preferenceService.save(tripId, userId, input);
    }

    /** 취향 조회. 저장 전이면 기본값과 saved=false. */
    @GetMapping
    public PreferenceView get(@PathVariable("tripId") long tripId,
                              @LoginUser Long userId) {
        return preferenceService.get(tripId, userId);
    }
}
