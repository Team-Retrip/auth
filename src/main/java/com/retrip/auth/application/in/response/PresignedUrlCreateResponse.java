package com.retrip.auth.application.in.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 Presigned Url 생성 Response")
public record PresignedUrlCreateResponse(

        @Schema(description = "이미지 업로드용 presigned url")
        String presignedUrl,

        @Schema(description = "읽기용 이미지 url (presignedUrl로 이미지 업로드 후 해당 imageUrl로 다운로드 가능하며 프로필 수정 시 해당 imageUrl 사용)")
        String readImageUrl
) {
    public static PresignedUrlCreateResponse of(String presignedUrl, String readImageUrl) {
        return new PresignedUrlCreateResponse(presignedUrl, readImageUrl);
    }
}
