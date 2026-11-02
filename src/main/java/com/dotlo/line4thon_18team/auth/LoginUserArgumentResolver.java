package com.dotlo.line4thon_18team.auth;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @LoginUser Long userId 에 값을 넣어준다.
 * 토큰 → (개발 모드일 때만) X-User-Id 헤더 → 둘 다 없으면 401 순서로 찾는다.
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String DEV_USER_HEADER = "X-User-Id";

    // 로컬 개발용. 배포 서버에는 이 환경변수가 없어서 자동으로 false
    private final boolean devHeaderEnabled;

    public LoginUserArgumentResolver(@Value("${auth.dev-header-enabled:false}") boolean devHeaderEnabled) {
        this.devHeaderEnabled = devHeaderEnabled;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 1. 토큰으로 로그인한 경우 (JwtAuthenticationFilter 가 넣어둔 사용자 id)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }

        // 2. 개발 모드에서만 X-User-Id 헤더 허용
        if (devHeaderEnabled) {
            String header = webRequest.getHeader(DEV_USER_HEADER);
            if (header != null && !header.isBlank()) {
                try {
                    return Long.valueOf(header.trim());
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, DEV_USER_HEADER + " 값이 올바르지 않습니다.");
                }
            }
        }

        // 3. 둘 다 없음
        throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
    }
}
