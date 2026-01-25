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

    // ... createToken, generateTokens 등 생성 로직은 기존 유지 ...
    // (위에 작성하신 코드 그대로 두셔도 됩니다. 아래 getAuthentication만 수정하면 됩니다.)

    public LoginResponse.TokenResponse generateTokens(Authentication authentication) {
        Instant now = Instant.now();
        String authorities = String.join(",", getAuthorities(authentication));

        String memberId = authentication.getName();
        String email = "";
        String name = "";
        String gender = null;
        Integer age = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            memberId = userDetails.getName();      // UUID
            email = userDetails.getEmail();
            name = userDetails.getRealName();
            gender = userDetails.getGender();
            age = userDetails.getAge();
        } else {
            // principal이 String인 경우 (방어 코드)
            memberId = authentication.getName();
        }

        String accessToken = createToken(memberId, email, name, gender, age, authorities, now, jwtConfig.getAccess().getExpireMin());
        String refreshToken = createToken(memberId, email, name, gender, age, authorities, now, jwtConfig.getRefresh().getExpireMin());

        return new LoginResponse.TokenResponse(accessToken, refreshToken);
    }

    private String createToken(String subject, String email, String name, String gender, Integer age,
                               String authorities, Instant issuedAt, long expirationMinutes) {
        try {
            PrivateKey privateKey = getPrivateKey(jwtConfig.getPrivateKey());
            Instant expiration = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);

            JwtBuilder builder = Jwts.builder()
                    .subject(subject)
                    .claim("username", email)
                    .claim("name", name)
                    .claim("authorities", authorities);

            if (gender != null) builder.claim("gender", gender);
            if (age != null) builder.claim("age", age);

            return builder
                    .issuedAt(Date.from(issuedAt))
                    .expiration(Date.from(expiration))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("토큰 생성 실패", e);
        }
    }

    // [중요 수정] Authentication 객체 생성 시 CustomUserDetails 재구성
    public Authentication getAuthentication(String token) {
        try {
            PublicKey publicKey = getPublicKey(jwtConfig.getPublicKey());
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 1. Claims에서 정보 추출
            String memberId = claims.getSubject(); // UUID
            String email = claims.get("username", String.class);
            String name = claims.get("name", String.class);
            String authoritiesStr = claims.get("authorities", String.class);
            String gender = claims.get("gender", String.class);
            Integer age = claims.get("age", Integer.class);

            // 2. 권한 목록 생성
            List<GrantedAuthority> authorities = Arrays.stream(authoritiesStr.split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // 3. 임시 Member 객체 생성 (비밀번호는 null 처리)
            Member member = Member.builder()
                    .id(UUID.fromString(memberId))
                    .email(new MemberEmail(email))
                    .name(new MemberName(name))
                    .gender(gender)
                    .age(age)
                    .password(null) // 인증된 상태이므로 비밀번호 불필요
                    .build();

            // 4. CustomUserDetails 생성
            CustomUserDetails principal = new CustomUserDetails(member);

            // 5. Authentication 리턴 (이제 Principal은 CustomUserDetails임)
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