package com.retrip.auth.application.in.response;

public record ChangePasswordResponse(
        String accessToken,
        String refreshToken
) {}