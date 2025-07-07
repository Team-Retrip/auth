package com.retrip.auth.application.in;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Test
    void 회원가입_성공() throws Exception {
        // given
        MemberCreateRequest request = new MemberCreateRequest("test@naver.com", "1234", "test");

        //when
        MemberCreateResponse response = memberService.createUser(request);

        //then
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("test");
        assertThat(response.email()).isEqualTo("test@naver.com");
    }

}
