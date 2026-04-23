package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증코드 발송 요청")
public record SendEmailCodeRequest(
        @Schema(description = "인증할 이메일", example = "user@example.com")
        @NotBlank @Email(message = "올바른 이메일 형식을 입력해주세요.")
        String email
) {}
