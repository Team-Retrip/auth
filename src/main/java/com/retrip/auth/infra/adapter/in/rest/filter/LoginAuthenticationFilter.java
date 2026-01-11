package com.retrip.auth.infra.adapter.in.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.config.UsernamePasswordAuthentication;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.RefreshToken;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class LoginAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final AuthenticationManager manager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 요청 검증
        String id = request.getHeader("id");
        String password = request.getHeader("password");

        if (!StringUtils.hasText(id) || !StringUtils.hasText(password)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // 2. 인증 시도
            Authentication authentication = new UsernamePasswordAuthentication(id, password);
            Authentication auth = manager.authenticate(authentication);

            // 3. 토큰 생성
            LoginResponse.TokenResponse tokenResponse = jwtProvider.generateTokens(auth);

            //  3-1. Refresh Token DB에 저장
            String authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            RefreshToken refreshToken = new RefreshToken(
                    tokenResponse.refreshToken(),
                    auth.getName(), // memberId
                    authorities
            );
            refreshTokenRepository.save(refreshToken);

            // 4. 응답 작성
            ApiResponse<LoginResponse.TokenResponse> result = ApiResponse.ok(tokenResponse);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(result));
            response.getWriter().flush();

        } catch (AuthenticationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !request.getRequestURI().equals("/login");
    }
}