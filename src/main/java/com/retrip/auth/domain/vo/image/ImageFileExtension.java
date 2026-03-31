package com.retrip.auth.domain.vo.image;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.retrip.auth.domain.exception.common.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

import static com.retrip.auth.domain.exception.common.ErrorCode.EXTENSION_NOT_FOUND;

@Getter
@AllArgsConstructor
public enum ImageFileExtension {

    JPEG,
    JPG,
    PNG,
    HEIC,
    HEIF,
    WEBP
    ;

    @JsonCreator
    public static ImageFileExtension ofName(String value) {
        return Arrays.stream(ImageFileExtension.values())
                .filter(extensionType -> extensionType.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new BusinessException(EXTENSION_NOT_FOUND));
    }
}
