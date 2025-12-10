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

    @Column(name = "email", nullable = true, length = EMAIL_LENGTH_LIMIT)
    private String value;

    public MemberEmail(String value) {

        if (value != null) {
            validate(value);
            this.value = value;
        }else {
            this.value = null;
        }
    }

    private void validate(String value) {
        if (value.length() > EMAIL_LENGTH_LIMIT) {
            throw new InvalidValueException("유저 ID는 " + EMAIL_LENGTH_LIMIT + "자를 넘을 수 없습니다.");
        }
    }

}
