package com.dotlo.line4thon_18team.auth.jwt;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * 우리 서비스 토큰(JWT)을 만들고 검사한다.
 * - access: API 요청마다 헤더에 넣는 토큰 (짧게)
 * - refresh: access 가 만료됐을 때 새로 받는 용도 (길게)
 * 두 토큰은 "type" 값으로 구분해서, refresh 토큰으로 API 를 부르는 걸 막는다.
 */
@Component
public class JwtProvider {

    private static final String ISSUER = "line4thon-18team";
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration accessValidity;
    private final Duration refreshValidity;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-minutes}") long accessMinutes,
            @Value("${jwt.refresh-token-validity-days}") long refreshDays) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 환경변수가 없습니다. .env 를 확인해 주세요.");
        }
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");

        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.accessValidity = Duration.ofMinutes(accessMinutes);
        this.refreshValidity = Duration.ofDays(refreshDays);
    }

    // ---------- 발급 ----------

    public String createAccessToken(Long userId) {
        return create(userId, ACCESS, accessValidity);
    }

    public String createRefreshToken(Long userId) {
        return create(userId, REFRESH, refreshValidity);
    }

    private String create(Long userId, String type, Duration validity) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plus(validity))
                .claim(TYPE_CLAIM, type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // ---------- 검사 (통과하면 사용자 id 반환) ----------

    public Long getUserIdFromAccessToken(String token) {
        return verify(token, ACCESS);
    }

    public Long getUserIdFromRefreshToken(String token) {
        return verify(token, REFRESH);
    }

    private Long verify(String token, String expectedType) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);   // 서명 + 만료 확인
        } catch (JwtValidationException e) {
            if (isExpired(e)) {
                throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
            }
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);   // 서명 불일치, 형식 오류 등
        }

        if (!expectedType.equals(jwt.getClaimAsString(TYPE_CLAIM))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);   // refresh 로 API 호출 등
        }
        return Long.valueOf(jwt.getSubject());
    }

    private static boolean isExpired(JwtValidationException e) {
        return e.getErrors().stream()
                .anyMatch(error -> error.getDescription() != null && error.getDescription().contains("expired"));
    }
}
