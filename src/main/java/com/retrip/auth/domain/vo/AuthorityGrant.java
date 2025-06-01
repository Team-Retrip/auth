package com.retrip.auth.domain.vo;

import com.retrip.auth.domain.exception.common.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Arrays;

import lombok.*;

@Getter
@AllArgsConstructor
public enum AuthorityGrant {
    ADMIN("admin", "관리자"),
    USER("user", "사용자"),
    ;

    private final String code;
    private final String viewName;
    public static AuthorityGrant codeOf(String code) {
        return Arrays.stream(AuthorityGrant.values())
                .filter(authorityGrant -> authorityGrant.getCode().equals(code))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코드입니다."));
    }

}
