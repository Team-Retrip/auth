package com.retrip.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추가 이메일 OTP 인증이 필요한 경우 반환 (HTTP 202)")
public record VerificationPendingResponse(
        @Schema(description = "이후 OTP 발송/확인 요청에 사용할 세션 ID")
        String sessionId
) {}
