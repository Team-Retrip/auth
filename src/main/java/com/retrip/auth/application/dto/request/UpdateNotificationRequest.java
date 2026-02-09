package com.retrip.auth.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateNotificationRequest {

    @NotNull(message = "알림 설정 값은 필수입니다.")
    private Boolean enabled;
}
