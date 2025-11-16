package com.retrip.auth.application.config;

import com.retrip.auth.application.in.response.LoginResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT 토큰의 생성 및 검증을 담당하는 클래스
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtConfig jwtConfig;

    /**
     * 인증 정보를 기반으로 Access Token과 Refresh Token을 생성합니다.
     *
     * @param authentication 인증 객체
     * @return 생성된 토큰 정보를 담은 TokenResponse 객체
     */
    public LoginResponse.TokenResponse generateTokens(Authentication authentication) {
        Instant now = Instant.now();
        String authorities = String.join(",", getAuthorities(authentication));

        String accessToken = createToken(
                authentication.getName(),
                authorities,
                now,
                jwtConfig.getAccess().getExpireMin()
        );

        String refreshToken = createToken(
                authentication.getName(),
                authorities,
                now,
                jwtConfig.getRefresh().getExpireMin()
        );

        return new LoginResponse.TokenResponse(accessToken, refreshToken);
    }

    private String createToken(String subject, String authorities, Instant issuedAt, long expirationMinutes) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        Instant expiration = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(subject)
                .claims(
                        Map.of(
                                "username", subject,
                                "authorities", authorities
                        )
                )
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    private List<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}