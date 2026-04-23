package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "이메일 인증코드 확인 요청")
public record VerifyEmailCodeRequest(
        @Schema(description = "인증할 이메일", example = "user@example.com")
        @NotBlank @Email(message = "올바른 이메일 형식을 입력해주세요.")
        String email,

        @Schema(description = "6자리 인증코드", example = "382910")
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자입니다.")
        String code
) {}
