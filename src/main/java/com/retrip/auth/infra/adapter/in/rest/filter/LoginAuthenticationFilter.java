package com.retrip.auth.infra.adapter.in.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.request.LoginRequest;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.RefreshToken;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

public class LoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginAuthenticationFilter(JwtConfig jwtConfig,
                                     AuthenticationManager authenticationManager,
                                     JwtProvider jwtProvider,
                                     RefreshTokenRepository refreshTokenRepository) {
        super.setAuthenticationManager(authenticationManager);

        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        setFilterProcessesUrl("/login"); // POST /login 요청을 가로채도록 설정
    }

    // 1. 로그인 시도
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.id(),
                    loginRequest.password()
            );

            // ★ [수정] 부모 클래스의 메서드를 통해 매니저를 호출
            return this.getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException("로그인 요청 파싱 실패", e);
        }
    }

    // 2. 로그인 성공 시
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {

        LoginResponse.TokenResponse tokenResponse = jwtProvider.generateTokens(authResult);

        // CustomUserDetails.getName()은 UUID String을 반환하도록 설정했으므로 바로 사용 가능
        String memberId = authResult.getName();

        RefreshToken refreshToken = new RefreshToken(
                tokenResponse.refreshToken(),
                memberId,
                "ROLE_USER"
        );
        refreshTokenRepository.save(refreshToken);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<LoginResponse.TokenResponse> apiResponse = ApiResponse.success(tokenResponse);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}