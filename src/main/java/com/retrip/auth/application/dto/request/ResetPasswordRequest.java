package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 재설정 요청")
public record ResetPasswordRequest(
        @NotBlank
        @Schema(description = "재설정 토큰")
        String token,

        @NotBlank
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]|;:,./]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다.")
        @Schema(description = "새 비밀번호 (영문+숫자+특수문자 8~20자)", example = "NewPass1!")
        String newPassword
) {}
