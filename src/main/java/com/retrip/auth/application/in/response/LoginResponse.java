package com.retrip.auth.application.in.response;

public record LoginResponse(
        TokenResponse token
) {
    public record TokenResponse(String accessToken, String refreshToken) {

    }
}
