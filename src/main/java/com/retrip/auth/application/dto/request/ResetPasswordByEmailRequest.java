package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 재설정 이메일 발송 요청")
public record ResetPasswordByEmailRequest(
        @NotBlank
        @Email
        @Schema(description = "가입 시 사용한 이메일", example = "user@example.com")
        String email
) {}
