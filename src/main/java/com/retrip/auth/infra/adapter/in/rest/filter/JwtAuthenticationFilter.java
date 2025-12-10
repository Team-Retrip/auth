package com.retrip.auth.infra.adapter.in.rest.filter;

import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.UsernamePasswordAuthentication;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";

    // [수정] 필터를 거치지 않을 경로에 '/auth/reissue', '/auth/logout' 추가
    private static final List<String> URI = List.of("/login", "/users", "/auth/reissue", "/auth/logout");

    private final JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getToken(request.getHeader(AUTHORIZATION_HEADER));
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        // [수정] 토큰이 없거나 유효하지 않으면, 그냥 다음 필터로 넘깁니다.
        // (401 에러는 SecurityConfig가 판단하게 맡김)
        if (token != null && validToken(token, key)) {
            Claims payload = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token).getPayload();
            String username = String.valueOf(payload.get("username"));
            List<GrantedAuthority> authorities = getAuthorities(String.valueOf(payload.get("authorities")));

            UsernamePasswordAuthentication auth = new UsernamePasswordAuthentication(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String getToken(String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private boolean validToken(String token, SecretKey key) {
        try {
            Jwts.parser()
                    .verifyWith(key)
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
        }
        return false;
    }

    private List<GrantedAuthority> getAuthorities(String authorities) {
        return Arrays.stream(authorities.split(","))
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // [수정] URI 리스트에 포함된 경로거나, Swagger 관련 경로는 필터 제외
        String path = request.getRequestURI();
        return URI.contains(path)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars");
    }
}