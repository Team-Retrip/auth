package com.retrip.auth.application.in.request;

import com.retrip.auth.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Member 회원가입 Request")
public record MemberUpdateRequest(
        @Schema(description = "이메일")
        String email,
        @Schema(description = "기존 비밀번호")
        String password,
        @Schema(description = "새로운 비밀번호")
        String newPassword,
        @Schema(description = "사용자 이름")
        String name
) {
}
