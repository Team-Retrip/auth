package com.retrip.auth.application.in.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]|;:,./]).{8,20}$",
                 message = "비밀번호는 영문·숫자·특수문자 포함 8~20자여야 합니다.")
        String newPassword
) {}