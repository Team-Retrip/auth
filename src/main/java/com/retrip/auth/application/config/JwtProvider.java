package com.retrip.auth.application.config;

import com.retrip.auth.application.in.response.LoginResponse;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import com.retrip.auth.application.config.CustomUserDetails;

/**
 * JWT 토큰의 생성(Sign) 및 검증(Verify)을 담당하는 클래스 (RSA 방식)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtConfig jwtConfig;

    /**
     * [생성] 인증 정보를 기반으로 RSA 서명된 Access/Refresh Token 생성
     */
    public LoginResponse.TokenResponse generateTokens(Authentication authentication) {
        Instant now = Instant.now();
        String authorities = String.join(",", getAuthorities(authentication));

        // [수정] Principal에서 ID(UUID)와 Email 추출 로직 추가
        String memberId = authentication.getName(); // 기본값
        String email = authentication.getName();    // 기본값

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            memberId = userDetails.getName();      // CustomUserDetails.getName()은 UUID(String) 반환
            email = userDetails.getUsername();     // CustomUserDetails.getUsername()은 이메일 반환
        }

        String accessToken = createToken(
                memberId,   // sub (UUID)
                email,      // claim: username (Email)
                authorities,
                now,
                jwtConfig.getAccess().getExpireMin()
        );

        String refreshToken = createToken(
                memberId,   // sub (UUID)
                email,      // claim: username (Email)
                authorities,
                now,
                jwtConfig.getRefresh().getExpireMin()
        );

        return new LoginResponse.TokenResponse(accessToken, refreshToken);
    }

    // [수정] 파라미터에 username(email) 추가
    private String createToken(String subject, String username, String authorities, Instant issuedAt, long expirationMinutes) {
        try {
            PrivateKey privateKey = getPrivateKey(jwtConfig.getPrivateKey());
            Instant expiration = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);

            return Jwts.builder()
                    .subject(subject) // 여기에는 UUID가 들어감
                    .claims(
                            Map.of(
                                    "username", username, // 여기에는 이메일이 들어감
                                    "authorities", authorities
                            )
                    )
                    .issuedAt(Date.from(issuedAt))
                    .expiration(Date.from(expiration))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("토큰 생성 실패", e);
        }
    }

    /**
     * [검증] 토큰 유효성 검사 (RSA Public Key 사용)
     */
    public boolean validateToken(String token) {
        try {
            PublicKey publicKey = getPublicKey(jwtConfig.getPublicKey());
            Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        } catch (Exception e) {
            log.error("JWT validation error", e);
        }
        return false;
    }

    /**
     * [파싱] 토큰에서 인증 객체 추출
     */
    public Authentication getAuthentication(String token) {
        try {
            PublicKey publicKey = getPublicKey(jwtConfig.getPublicKey());
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.get("username", String.class);
            String authoritiesStr = claims.get("authorities", String.class);

            List<GrantedAuthority> authorities = Arrays.stream(authoritiesStr.split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // UserContext나 CustomUserDetails 대신 표준 토큰 객체 사용
            return new UsernamePasswordAuthenticationToken(username, null, authorities);

        } catch (Exception e) {
            throw new RuntimeException("인증 정보 추출 실패", e);
        }
    }

    // --- Key Parsing Helpers ---

    private PrivateKey getPrivateKey(String key) throws Exception {
        String sanitizedKey = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(sanitizedKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey getPublicKey(String key) throws Exception {
        String sanitizedKey = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(sanitizedKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private List<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}