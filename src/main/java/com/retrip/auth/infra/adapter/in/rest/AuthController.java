package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.in.AuthService; // 새로 만든 서비스 import
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import com.retrip.auth.infra.adapter.in.rest.common.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService; // AuthService 주입
    private final JwtConfig jwtConfig;
    private final RefreshTokenRepository refreshTokenRepository; // 로그아웃용

    /**
     * 토큰 재발급 (Silent Refresh + RTR)
     */
    @PostMapping("/reissue")
    public ApiResponse<Map<String, String>> reissue(
            @CookieValue(name = "refreshToken", required = false) String requestRefreshToken,
            HttpServletResponse response
    ) {
        // 서비스 호출 (트랜잭션 처리는 서비스 내부에서 수행)
        LoginResponse.TokenResponse newToken = authService.reissue(requestRefreshToken);

        // 쿠키 재설정
        CookieUtils.addCookie(
                response,
                "refreshToken",
                newToken.refreshToken(),
                jwtConfig.getRefresh().getExpireMin() * 60
        );

        // Access Token 반환
        return ApiResponse.ok(Map.of("accessToken", newToken.accessToken()));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String requestRefreshToken,
            HttpServletResponse response
    ) {
        if (requestRefreshToken != null) {
            refreshTokenRepository.findByTokenValue(requestRefreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
        CookieUtils.deleteCookie(response, "refreshToken");
        return ApiResponse.noContent();
    }
}