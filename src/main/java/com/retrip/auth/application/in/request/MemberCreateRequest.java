package com.retrip.auth.application.in.request;

import com.retrip.auth.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Member 회원가입 Request")
public record MemberCreateRequest(
        @Schema(description = "이메일")
        String email,
        @Schema(description = "비밀번호")
        String password,
        @Schema(description = "사용자 이름")
        String name,
        @Schema(description = "성별 (M/F)")
        String gender,
        @Schema(description = "생년월일 (YYYY-MM-DD)")
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
                marketingAgreed
        );
    }
}
