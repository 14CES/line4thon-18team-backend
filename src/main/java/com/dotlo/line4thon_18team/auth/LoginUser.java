package com.dotlo.line4thon_18team.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러에서 로그인한 사용자 id 를 받는다. 이걸 쓴 API 는 로그인이 필요하다.
 * 예) public ResponseEntity<...> getMyTrips(@LoginUser Long userId)
 *
 * - access 토큰이 있으면 토큰의 사용자 id
 * - 토큰이 없고 개발 모드(AUTH_DEV_HEADER_ENABLED=true)면 X-User-Id 헤더 값
 * - 둘 다 없으면 401 LOGIN_REQUIRED
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
