package com.retrip.auth.application.in.request;

import com.retrip.auth.domain.vo.image.ImageFileExtension;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이미지 Presigned Url 생성 Request")
public record PresignedUrlCreateRequest(

        @Schema(description = "이미지 확장자", example = "JPG")
        @NotNull
        ImageFileExtension extension
) {
}
