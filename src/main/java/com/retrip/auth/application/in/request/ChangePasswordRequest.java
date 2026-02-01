package com.retrip.auth.application.in.request;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}