package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.in.request.PresignedUrlCreateRequest;
import com.retrip.auth.application.in.response.PresignedUrlCreateResponse;
import com.retrip.auth.application.in.usercase.ImageManageUseCase;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Image", description = "이미지 관련 API")
@RequiredArgsConstructor
@RequestMapping("/images")
@RestController
public class ImageController {

    private final ImageManageUseCase imageManageUseCase;

    @Operation(summary = "프로필 이미지 업로드용 Presigned URL 발급",
            description = "S3 업로드용 presigned URL과 업로드 후 사용할 readImageUrl을 반환합니다. readImageUrl을 프로필 수정 시 profileImageUrl로 사용하세요.")
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUrlCreateResponse> createImagePresignedUrl(
            Authentication authentication,
            @RequestBody PresignedUrlCreateRequest request) {
        UUID memberId = UUID.fromString((String) authentication.getPrincipal());
        PresignedUrlCreateResponse response = imageManageUseCase.createImagePresignedUrl(memberId, request.extension());
        return ApiResponse.created(response);
    }
}
