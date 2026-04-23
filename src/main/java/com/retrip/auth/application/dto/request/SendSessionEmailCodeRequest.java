package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "본인인증 세션 이메일 OTP 발송 요청 (비밀번호 재설정 전용)")
public record SendSessionEmailCodeRequest(
        @Schema(description = "인증 세션 ID")
        @NotBlank
        String sessionId,

        @Schema(description = "가입 시 사용한 이메일", example = "user@example.com")
        @NotBlank @Email(message = "올바른 이메일 형식을 입력해주세요.")
        String email
) {}
