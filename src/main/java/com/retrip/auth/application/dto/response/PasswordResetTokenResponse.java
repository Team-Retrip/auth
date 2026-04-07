package com.retrip.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비밀번호 재설정 토큰 응답 (본인인증 경로)")
public record PasswordResetTokenResponse(
        @Schema(description = "비밀번호 재설정 토큰 (30분 유효, 1회용)")
        String resetToken
) {}
