package com.retrip.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "아이디 찾기 응답")
public record FindEmailResponse(
        @Schema(description = "마스킹된 이메일", example = "us**@example.com")
        String maskedEmail,

        @Schema(description = "연결된 로그인 제공자 목록 (local, google, kakao 등)", example = "[\"google\", \"kakao\"]")
        List<String> providers,

        @Schema(description = "이번 찾기 과정에서 본인인증이 새로 연결된 경우 true")
        boolean isNowVerified
) {
    public static FindEmailResponse of(String email, List<String> providers, boolean isNowVerified) {
        return new FindEmailResponse(maskEmail(email), providers, isNowVerified);
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return email;

        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        int showLength = Math.min(3, Math.max(1, (local.length() + 1) / 2));
        String masked = local.substring(0, showLength) + "*".repeat(local.length() - showLength);
        return masked + domain;
    }
}
