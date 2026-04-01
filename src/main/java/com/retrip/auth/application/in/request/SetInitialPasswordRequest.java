package com.retrip.auth.application.in.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "소셜 유저 최초 비밀번호 설정 Request")
public record SetInitialPasswordRequest(
        @Schema(description = "설정할 비밀번호")
        @NotBlank
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]|;:,./]).{8,20}$",
                 message = "비밀번호는 영문·숫자·특수문자 포함 8~20자여야 합니다.")
        String password
) {
}
