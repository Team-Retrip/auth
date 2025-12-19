package com.retrip.auth.infra.adapter.in.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.JwtProvider; // [추가]
import com.retrip.auth.application.config.UsernamePasswordAuthentication;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LoginAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final AuthenticationManager manager;
    private final JwtProvider jwtProvider; // [추가] 토큰 생성기 주입

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 검증
        String id = request.getHeader("id");
        String password = request.getHeader("password");

        if (!StringUtils.hasText(id) || !StringUtils.hasText(password)) {
            // 값이 없으면 400 Bad Request가 더 적절하지만, 기존 로직 유지
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // 2. 인증 시도
            Authentication authentication = new UsernamePasswordAuthentication(id, password);
            Authentication auth = manager.authenticate(authentication);

            // 3. [핵심 변경] JwtProvider를 통해 RSA 서명된 토큰 생성
            // 기존의 복잡한 generateToken 메서드 삭제 -> 위임
            LoginResponse.TokenResponse tokenResponse = jwtProvider.generateTokens(auth);

            // 4. 응답 작성
            ApiResponse<LoginResponse.TokenResponse> result = ApiResponse.ok(tokenResponse);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(result));
            response.getWriter().flush();

        } catch (AuthenticationException e) {
            // 인증 실패 시 처리
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // /login 경로만 필터 적용 (나머지는 통과 -> true 반환)
        return !request.getRequestURI().equals("/login");
    }
}