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
public class MemberEmail {
    private static final int EMAIL_LENGTH_LIMIT = 50;

    @Column(name = "email", nullable = false, length = EMAIL_LENGTH_LIMIT)
    private String value;

    public MemberEmail(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value.length() > EMAIL_LENGTH_LIMIT) {
            throw new InvalidValueException("유저 ID는 " + EMAIL_LENGTH_LIMIT + "자를 넘을 수 없습니다.");
        }
    }

}
