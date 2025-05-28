package com.retrip.auth.application.in;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.retrip.auth.application.in.base.BaseMemberServiceTest;
import com.retrip.auth.application.in.request.LoginRequest;
import com.retrip.auth.common.fixture.MemberFixture;

import org.junit.jupiter.api.Test;

class MemberServiceTest extends BaseMemberServiceTest {

    @Test
    public void 로그인_테스트() {
        //given
        memberRepository.save(member);
        LoginRequest request = MemberFixture.loginRequest("test@naver.com", "1234");

        //when

        //then
        assertThatCode(() -> memberService.login(request)).doesNotThrowAnyException();
    }

}
