package com.retrip.auth.domain.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import com.retrip.auth.domain.exception.PasswordNotMatchException;
import com.retrip.auth.domain.vo.MemberPassword;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberTest {

    @Test
    public void 패스워드_매처_성공_테스트(){
        //given
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        MemberPassword mp = new MemberPassword(passwordEncoder.encode( "TEST"));

        //when
        String password = "TEST";



        //then
        assertThatCode(() -> mp.matches(passwordEncoder, password))
                .doesNotThrowAnyException();
    }

    @Test
    public void 패스워드_매처_실패_테스트(){
        //given
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        MemberPassword mp = new MemberPassword(passwordEncoder.encode( "TEST"));

        //when
        String password = "FAIL";

        //then
        assertThatThrownBy(() -> mp.matches(passwordEncoder, passwordEncoder.encode(password)))
                .isExactlyInstanceOf(PasswordNotMatchException.class);
    }
}
