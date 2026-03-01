package com.retrip.auth.application.in.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Member 정보 수정 Request")
public record MemberUpdateRequest(
        @Schema(description = "기존 비밀번호")
        String password,
        @Schema(description = "새로운 비밀번호")
        String newPassword,
        @Schema(description = "사용자 이름")
        String name,
        @Schema(description = "성별 (M/F)")
        String gender,
        @Schema(description = "생년월일 (YYYY-MM-DD)")
        String birthDate
) {
}
