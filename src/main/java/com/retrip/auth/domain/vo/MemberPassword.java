package com.retrip.auth.domain.vo;

import com.retrip.auth.domain.exception.PasswordNotMatchException;
import com.retrip.auth.domain.exception.common.InvalidValueException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class MemberPassword {
    private static final int PASSWORD_LENGTH_LIMIT = 60;

    @Column(name = "password", nullable = false, length = PASSWORD_LENGTH_LIMIT)
    private String value;

    public MemberPassword(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value.length() > PASSWORD_LENGTH_LIMIT) {
            throw new InvalidValueException("유저 비밀번호는 " + PASSWORD_LENGTH_LIMIT + "자를 넘을 수 없습니다.");
        }
    }

    public void matches(PasswordEncoder passwordEncoder, String password) {
        if (!passwordEncoder.matches(password, this.value)){
            throw new PasswordNotMatchException();
        }
    }
}
