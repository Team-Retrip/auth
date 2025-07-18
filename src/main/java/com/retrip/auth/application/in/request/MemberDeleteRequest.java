package com.retrip.auth.application.in.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Member 삭제 Request")
public record MemberDeleteRequest(
        @Schema(description = "이메일")
        String email,
        @Schema(description = "비밀번호")
        String password
) {
}
