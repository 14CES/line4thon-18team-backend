package com.dotlo.line4thon_18team.global.config;

import com.dotlo.line4thon_18team.auth.jwt.JwtAuthenticationFilter;
import com.dotlo.line4thon_18team.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * 보안 설정.
 * - 요청마다 access 토큰이 있으면 검사해서 사용자 id 를 기억한다. (JwtAuthenticationFilter)
 * - 로그인이 필요한지는 API 마다 @LoginUser 로 판단한다. 그래서 여기서는 모든 요청을 통과시킨다.
 *
 * @WebMvcTest 에서는 이 설정이 자동으로 안 불러와져서, 테스트에
 * @Import({SecurityConfig.class, JwtProvider.class}) 를 붙여야 한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                 // 토큰 방식이라 CSRF 보호 불필요
                .httpBasic(basic -> basic.disable())          // 브라우저 기본 로그인 창 끄기
                .formLogin(form -> form.disable())            // 로그인 페이지 끄기
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 세션 대신 토큰
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())            // 로그인 확인은 @LoginUser 에서
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, objectMapper),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
