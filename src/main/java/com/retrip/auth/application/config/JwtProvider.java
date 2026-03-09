package com.retrip.auth.application.config;

import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.vo.MemberEmail;
import com.retrip.auth.domain.vo.MemberName;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtConfig jwtConfig;

    public LoginResponse.TokenResponse generateTokens(Authentication authentication) {
        Instant now = Instant.now();
        String authorities = String.join(",", getAuthorities(authentication));

        String memberId = authentication.getName();
        String email = "";
        String name = "";
        String gender = null;
        String birthDate = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            memberId = userDetails.getName();
            email = userDetails.getEmail();
            name = userDetails.getRealName();
            gender = userDetails.getGender();
            birthDate = userDetails.getBirthDate();
        } else {
            memberId = authentication.getName();
        }

        String accessToken = createToken(memberId, email, name, gender, birthDate, authorities, now, jwtConfig.getAccess().getExpireMin());
        String refreshToken = createToken(memberId, email, name, gender, birthDate, authorities, now, jwtConfig.getRefresh().getExpireMin());

        return new LoginResponse.TokenResponse(accessToken, refreshToken);
    }

    private String createToken(String subject, String email, String name, String gender, String birthDate,
                               String authorities, Instant issuedAt, long expirationMinutes) {
        try {
            PrivateKey privateKey = getPrivateKey(jwtConfig.getPrivateKey());
            Instant expiration = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);

            JwtBuilder builder = Jwts.builder()
                    .subject(subject)
                    .claim("username", email)
                    .claim("name", name)
                    .claim("authorities", authorities); // 권한이 없으면 빈 문자열 ""이 들어감

            if (gender != null) builder.claim("gender", gender);
            if (birthDate != null) builder.claim("birthDate", birthDate);

            return builder
                    .issuedAt(Date.from(issuedAt))
                    .expiration(Date.from(expiration))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("토큰 생성 실패", e);
        }
    }

    public Authentication getAuthentication(String token) {
        try {
            PublicKey publicKey = getPublicKey(jwtConfig.getPublicKey());
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 1. Claims에서 정보 추출
            String memberId = claims.getSubject();
            String email = claims.get("username", String.class);
            String name = claims.get("name", String.class);
            String authoritiesStr = claims.get("authorities", String.class);
            String gender = claims.get("gender", String.class);
            String birthDate = claims.get("birthDate", String.class);

            // 2. 권한 목록 생성 [수정된 부분: 빈 문자열 처리 추가]
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (authoritiesStr != null && !authoritiesStr.isBlank()) {
                authorities = Arrays.stream(authoritiesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty()) // 빈 문자열 필터링 (중요)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            // 3. 임시 Member 객체 생성
            Member member = Member.builder()
                    .id(UUID.fromString(memberId))
                    .email(new MemberEmail(email))
                    .name(new MemberName(name))
                    .gender(gender)
                    .birthDate(birthDate)
                    .password(null)
                    .build();

            // 4. CustomUserDetails 생성
            CustomUserDetails principal = new CustomUserDetails(member);

            return new UsernamePasswordAuthenticationToken(principal, token, authorities);

        } catch (Exception e) {
            throw new RuntimeException("인증 정보 추출 실패", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            PublicKey publicKey = getPublicKey(jwtConfig.getPublicKey());
            Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    public Claims parseClaims(String token) {
        try {
            PublicKey publicKey = getPublicKey(jwtConfig.getPublicKey());
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (Exception e) {
            throw new RuntimeException("토큰 파싱 실패", e);
        }
    }

    private PrivateKey getPrivateKey(String key) throws Exception {
        String sanitizedKey = key.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(sanitizedKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey getPublicKey(String key) throws Exception {
        String sanitizedKey = key.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
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
