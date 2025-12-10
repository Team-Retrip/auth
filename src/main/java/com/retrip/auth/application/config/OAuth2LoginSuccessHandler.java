package com.retrip.auth.application.config;

import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.RefreshToken;
import com.retrip.auth.infra.adapter.in.rest.common.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final RefreshTokenRepository refreshTokenRepository;

    // TODO: 배포 환경에 따라 도메인 변경 필요 (application.yml에서 관리 권장)
    private static final String FRONTEND_CALLBACK_URL = "http://localhost:3000/auth/callback";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login Successful. Generating Tokens...");

        // 1. JWT 토큰 생성 (Access + Refresh)
        LoginResponse.TokenResponse tokenResponse = jwtProvider.generateTokens(authentication);

        // 2. 사용자 정보 추출 (Member ID)
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID memberId = userDetails.getMember().getId();

        // 3. Refresh Token DB 저장 (RTR 적용)
        saveRefreshToken(memberId, tokenResponse.refreshToken());

        // 4. Refresh Token 쿠키 발급 (HttpOnly, Secure)
        int refreshTokenExpireSeconds = jwtConfig.getRefresh().getExpireMin() * 60;
        CookieUtils.addCookie(
                response,
                "refreshToken",
                tokenResponse.refreshToken(),
                refreshTokenExpireSeconds
        );

        // 5. 프론트엔드로 리다이렉트 (Access Token만 파라미터로 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(FRONTEND_CALLBACK_URL)
                .queryParam("accessToken", tokenResponse.accessToken())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        log.info("Redirecting to Frontend: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }

    private void saveRefreshToken(UUID memberId, String tokenValue) {
        long refreshTokenExpireMillis = System.currentTimeMillis() + (jwtConfig.getRefresh().getExpireMin() * 60 * 1000L);

        RefreshToken refreshToken = RefreshToken.create(
                memberId,
                tokenValue,
                refreshTokenExpireMillis
        );

        refreshTokenRepository.save(refreshToken);
    }
}