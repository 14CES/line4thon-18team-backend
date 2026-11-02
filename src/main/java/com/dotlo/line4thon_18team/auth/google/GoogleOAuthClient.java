package com.dotlo.line4thon_18team.auth.google;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 구글 로그인 code 로 "이 사람이 누구인지" 확인한다.
 *
 * 1. code + 클라이언트 정보를 구글 토큰 주소에 보내서 토큰을 받는다.
 * 2. 받은 id_token(구글이 서명한 JWT)의 서명·발급 대상·만료를 검증한다.
 * 3. id_token 안의 sub, email, name, picture 를 꺼낸다.
 */
@Slf4j
@Component
public class GoogleOAuthClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String JWK_SET_URL = "https://www.googleapis.com/oauth2/v3/certs";  // 구글 서명 확인용 공개키
    private static final String ISSUER = "https://accounts.google.com";

    // 구글 토큰 응답 중 필요한 필드만 (구글은 snake_case 로 보냄)
    record TokenResponse(
            @JsonProperty("id_token") String idToken,
            @JsonProperty("refresh_token") String refreshToken
    ) {}

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestClient restClient = RestClient.create();
    private final JwtDecoder idTokenDecoder;

    public GoogleOAuthClient(
            @Value("${google.oauth.client-id}") String clientId,
            @Value("${google.oauth.client-secret}") String clientSecret,
            @Value("${google.oauth.redirect-uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.idTokenDecoder = createIdTokenDecoder(clientId);
    }

    public GoogleUser login(String code) {
        TokenResponse token = exchangeCode(code);
        Jwt idToken = verifyIdToken(token.idToken());

        return new GoogleUser(
                idToken.getSubject(),
                idToken.getClaimAsString("email"),
                idToken.getClaimAsString("name"),
                idToken.getClaimAsString("picture"),
                token.refreshToken()
        );
    }

    // ---------- 1. code → 구글 토큰 ----------

    private TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        // 브라우저 주소창에서 복사하면 "4%2F0A..." 처럼 인코딩된 채로 올 수 있어서 원래 모양으로 되돌린다
        form.add("code", URLDecoder.decode(code, StandardCharsets.UTF_8));
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            TokenResponse token = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (token == null || token.idToken() == null) {
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
            }
            return token;
        } catch (RestClientException e) {
            // code 만료·재사용, 리디렉션 주소 불일치, 잘못된 클라이언트 정보 등
            log.warn("구글 토큰 교환 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    // ---------- 2. id_token 검증 ----------

    private Jwt verifyIdToken(String idToken) {
        try {
            return idTokenDecoder.decode(idToken);
        } catch (JwtException e) {
            log.warn("구글 ID 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    // 구글 공개키로 서명 확인 + 발급자(iss)·만료(exp) 확인 + 우리 앱용 토큰인지(aud) 확인
    private static JwtDecoder createIdTokenDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URL).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(clientId))
        ));
        return decoder;
    }
}
