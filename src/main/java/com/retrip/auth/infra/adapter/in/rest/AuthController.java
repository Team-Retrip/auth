package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.in.AuthService;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.domain.exception.common.InvalidValueException;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/reissue")
    public ApiResponse<LoginResponse.TokenResponse> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new InvalidValueException("Refresh Token이 쿠키에 없습니다.");
        }

        LoginResponse.TokenResponse tokenResponse = authService.reissue(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenResponse.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(14 * 24 * 60 * 60) // 14일
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ApiResponse.ok(tokenResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 쿠키 삭제 (만료시간 0)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSecure ? "None" : "Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ApiResponse.ok(null);
    }
}