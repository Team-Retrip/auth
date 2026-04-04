package com.retrip.auth.application.in.request;

import com.retrip.auth.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Member 회원가입 Request")
public record MemberCreateRequest(
        @Schema(description = "이메일")
        @NotBlank @Email(message = "올바른 이메일 형식을 입력해주세요.")
        String email,
        @Schema(description = "비밀번호")
        @NotBlank
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]|;:,./]).{8,20}$",
                 message = "비밀번호는 영문·숫자·특수문자 포함 8~20자여야 합니다.")
        String password,
        @Schema(description = "사용자 이름")
        @NotBlank(message = "이름은 필수입니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z ]+$", message = "이름에는 숫자나 특수문자를 포함할 수 없습니다.")
        @Size(max = 10, message = "이름은 최대 10자까지 입력할 수 있습니다.")
        String name,
        @Schema(description = "닉네임 (선택, 미입력 시 이름으로 자동 설정)")
        @Size(max = 15, message = "닉네임은 최대 15자까지 입력할 수 있습니다.")
        String nickname,
        @Schema(description = "성별 (M/F)")
        @NotBlank(message = "성별은 필수입니다.")
        @Pattern(regexp = "^[MF]$", message = "성별은 M 또는 F여야 합니다.")
        String gender,
        @Schema(description = "생년월일 (YYYY-MM-DD)")
        @NotBlank(message = "생년월일은 필수입니다.")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$",
                 message = "생년월일은 YYYY-MM-DD 형식이어야 합니다.")
        String birthDate,
        @Schema(description = "필수 약관 동의")
        boolean termsAgreed,
        @Schema(description = "마케팅 수신 동의")
        boolean marketingAgreed
) {
    public Member to(String encodePassword) {
        return Member.create(
                name,
                email,
                encodePassword,
                List.of("user"),
                gender,
                birthDate,
                termsAgreed,
                marketingAgreed,
                nickname
        );
    }
}
