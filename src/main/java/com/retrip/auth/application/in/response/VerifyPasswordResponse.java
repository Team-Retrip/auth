package com.retrip.auth.application.in.response;

public record VerifyPasswordResponse(
        boolean isValid
) {}