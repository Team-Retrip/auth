package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "본인인증 세션 OTP 확인 요청")
public record ConfirmSessionRequest(
        @Schema(description = "인증 세션 ID")
        @NotBlank
        String sessionId,

        @Schema(description = "6자리 인증코드", example = "382910")
        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "인증코드는 6자리 숫자입니다.")
        String code
) {}
