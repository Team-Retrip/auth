package com.retrip.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyIdentityRequest {

    @NotBlank(message = "imp_uid는 필수입니다.")
    private String impUid;
}
