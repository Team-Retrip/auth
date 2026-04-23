package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "세션 ID만 필요한 요청")
public record SessionIdRequest(
        @Schema(description = "인증 세션 ID")
        @NotBlank
        String sessionId
) {}
