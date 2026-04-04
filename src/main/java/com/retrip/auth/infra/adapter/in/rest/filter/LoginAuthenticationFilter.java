package com.retrip.auth.infra.adapter.in.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.request.LoginRequest;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
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

import org.springframework.http.ResponseCookie;

import java.io.IOException;

public class LoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean cookieSecure;

    public LoginAuthenticationFilter(JwtConfig jwtConfig,
                                     AuthenticationManager authenticationManager,
                                     JwtProvider jwtProvider,
                                     RefreshTokenRepository refreshTokenRepository,
                                     MemberRepository memberRepository,
                                     boolean cookieSecure) {
        super.setAuthenticationManager(authenticationManager);
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberRepository = memberRepository;
        this.cookieSecure = cookieSecure;
        setFilterProcessesUrl("/login");
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
            return this.getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException("로그인 요청 파싱 실패", e);
        }
    }

    // 2. 로그인 성공 시
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {

        // 마지막 로그인 방식 업데이트
        if (authResult.getPrincipal() instanceof CustomUserDetails userDetails) {
            memberRepository.findById(userDetails.getMember().getId()).ifPresent(member -> {
                member.updateLastLoginProvider("local");
                memberRepository.save(member);
            });
        }

        LoginResponse.TokenResponse tokenResponse = jwtProvider.generateTokens(authResult);
        String memberId = authResult.getName();

        RefreshToken refreshToken = new RefreshToken(
                tokenResponse.refreshToken(),
                memberId,
                "ROLE_USER"
        );
        refreshTokenRepository.save(refreshToken);

        // refreshToken을 HttpOnly 쿠키로 설정
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenResponse.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(14 * 24 * 60 * 60)
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<LoginResponse.TokenResponse> apiResponse = ApiResponse.success(tokenResponse);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
