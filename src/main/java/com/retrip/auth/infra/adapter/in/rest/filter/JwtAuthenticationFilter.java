package com.retrip.auth.infra.adapter.in.rest.filter;

import com.retrip.auth.application.config.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component; // [필수]
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    // [유지] 사용자 정의 제외 URI
    private static final List<String> URI = List.of("/login", "/users");

    //  JwtConfig 대신 JwtProvider를 주입받아 사용 (책임 분리)
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getToken(request.getHeader(AUTHORIZATION_HEADER));

        // 토큰이 유효한 경우에만 인증 처리
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            // 유효하면 인증 객체 생성 후 SecurityContext에 저장
            Authentication auth = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        // 토큰이 없거나 유효하지 않으면 그냥 통과시킴 (SecurityConfig의 .authenticated()에서 걸러짐)
        // 만약 여기서 401을 직접 리턴하고 싶다면 else 블록에서 처리하고 return 해야 함.
        // 현재 로직은 "인증 정보가 있으면 넣고, 없으면 안 넣고 다음 필터로 넘김" 방식입니다.

        filterChain.doFilter(request, response);
    }

    private String getToken(String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // [유지] 기존에 작성하신 제외 로직 그대로 사용
        String path = request.getRequestURI();
        return URI.contains(path)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars");
    }
}