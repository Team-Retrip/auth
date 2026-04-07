package com.retrip.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "아이디 찾기 요청 (본인인증)")
public record FindEmailByVerificationRequest(
        @NotBlank
        @Schema(description = "PortOne 본인인증 impUid", example = "imp_1234567890")
        String impUid
) {}
