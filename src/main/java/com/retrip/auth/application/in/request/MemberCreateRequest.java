package com.retrip.auth.application.in.request;

import com.retrip.auth.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "나이")
        Integer age
) {
    public Member to(String encodePassword) {
        return Member.builder()
                .email(new com.retrip.auth.domain.vo.MemberEmail(email))
                .password(new com.retrip.auth.domain.vo.MemberPassword(encodePassword))
                .name(new com.retrip.auth.domain.vo.MemberName(name))
                .gender(gender)
                .age(age)
                .isDeleted(false)
                .provider("local")
                .isVerified(false)
                .id(java.util.UUID.randomUUID())
                .build();
    }
}