package com.retrip.auth.domain.vo;

import com.retrip.auth.domain.exception.common.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class MemberPassword {
    private static final int PASSWORD_LENGTH_LIMIT = 60;

    // [수정] 소셜 로그인을 위해 nullable = true로 변경
    @Column(name = "password", nullable = true, length = PASSWORD_LENGTH_LIMIT)
    private String value;

    public MemberPassword(String value) {
        // [신규] value가 null이 아닐 때만 유효성 검사 수행
        if (value != null) {
            validate(value);
            this.value = value;
        } else {
            this.value = null;
        }
    }

    private void validate(String value) {
        if (value.length() > PASSWORD_LENGTH_LIMIT) {
            throw new InvalidValueException("유저 비밀번호는 " + PASSWORD_LENGTH_LIMIT + "자를 넘을 수 없습니다.");
        }
    }
}