package com.retrip.auth.application.in.usercase;

import com.retrip.auth.application.in.response.PresignedUrlCreateResponse;
import com.retrip.auth.domain.vo.image.ImageFileExtension;

import java.util.UUID;

public interface ImageManageUseCase {
    PresignedUrlCreateResponse createImagePresignedUrl(UUID memberId, ImageFileExtension extension);
}
