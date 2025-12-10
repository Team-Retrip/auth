package com.retrip.auth.infra.adapter.in.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.config.UsernamePasswordAuthentication;
import com.retrip.auth.application.in.request.LoginRequest;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.RefreshToken;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import com.retrip.auth.infra.adapter.in.rest.common.CookieUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class LoginAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager manager;
    private final RefreshTokenRepository refreshTokenRepository; // [신규] DB 저장용
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 로그인 요청이 아니면 필터 통과
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Request Body에서 ID/PW 파싱 (기존 Header 방식 -> Body JSON 방식으로 변경)
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            // 3. 인증 시도
            Authentication authentication = new UsernamePasswordAuthentication(loginRequest.email(), loginRequest.password());
            Authentication authResult = manager.authenticate(authentication);

            // 4. 인증 성공 처리 (토큰 발급 및 쿠키 설정)
            onAuthenticationSuccess(response, authResult);

        } catch (AuthenticationException e) {
            log.error("Authentication failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.of(null, org.springframework.http.HttpStatus.UNAUTHORIZED)));
        }
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getRequestURI());
    }

    private void onAuthenticationSuccess(HttpServletResponse response, Authentication authResult) throws IOException {
        // 1. 토큰 공장(JwtProvider) 가동!
        LoginResponse.TokenResponse tokenResponse = jwtProvider.generateTokens(authResult);

        // 2. User 정보 추출
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
        UUID memberId = userDetails.getMember().getId();

        // 3. Refresh Token DB 저장 (RTR)
        long refreshTokenExpireMillis = jwtConfig.getRefresh().getExpireMin() * 60 * 1000L;
        RefreshToken refreshTokenEntity = RefreshToken.create(
                memberId,
                tokenResponse.refreshToken(),
                System.currentTimeMillis() + refreshTokenExpireMillis
        );
        refreshTokenRepository.save(refreshTokenEntity);

        // 4. Refresh Token 쿠키 설정
        CookieUtils.addCookie(
                response,
                "refreshToken",
                tokenResponse.refreshToken(),
                jwtConfig.getRefresh().getExpireMin() * 60
        );

        // 5. Access Token만 Body로 반환
        Map<String, String> responseData = Map.of("accessToken", tokenResponse.accessToken());
        ApiResponse<Map<String, String>> apiResponse = ApiResponse.ok(responseData);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

}


