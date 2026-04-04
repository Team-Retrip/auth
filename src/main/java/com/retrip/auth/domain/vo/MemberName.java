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
public class MemberName {
    private static final int NAME_LENGTH_LIMIT = 10;

    @Column(name = "name", nullable = false, length = NAME_LENGTH_LIMIT)
    private String value;

    public MemberName(String value) {
        validate(value);
        this.value = value;
    }

    private static final String NAME_PATTERN = "^[가-힣a-zA-Z ]+$";

    private void validate(String value) {
        if (value.isBlank()) {
            throw new InvalidValueException("유저 이름은 공백일 수 없습니다.");
        }
        if (value.length() > NAME_LENGTH_LIMIT) {
            throw new InvalidValueException("유저 이름은 " + NAME_LENGTH_LIMIT + "자를 넘을 수 없습니다.");
        }
        if (!value.matches(NAME_PATTERN)) {
            throw new InvalidValueException("유저 이름에는 숫자나 특수문자를 포함할 수 없습니다.");
        }
    }

}
