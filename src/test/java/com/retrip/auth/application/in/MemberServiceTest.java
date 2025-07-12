package com.retrip.auth.application.in;


import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.retrip.auth.application.in.factory.BaseMemberServiceTest;
import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.request.MemberUpdateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import com.retrip.auth.application.in.response.MemberUpdateResponse;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.sun.jdi.request.DuplicateRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class MemberServiceTest extends BaseMemberServiceTest {

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

    @Test
    void 회원가입_중복_회원가입() throws Exception {
        // given
        memberRepository.save(Member.create("test", "test@naver.com", "1234"));
        MemberCreateRequest request = new MemberCreateRequest("test@naver.com", "1111", "중복 테스트");

        //when

        //then
        assertThatThrownBy(() -> memberService.createUser(request))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessageContaining("Email already exists");
    }


    @Test
    void 회원정보_수정_패스워드_달라_실패() throws Exception {
        // given
        memberRepository.save(Member.create("test", "test@naver.com", "1234"));
        MemberUpdateRequest request = new MemberUpdateRequest("test@naver.com", "1235", "1111",
                "수정 테스트");

        //when

        //then
        assertThatThrownBy(() -> memberService.updateUser(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");
    }

    @Test
    void 저장된_회원_정보가_없어서_실패() throws Exception {
        // given
        MemberUpdateRequest request = new MemberUpdateRequest("test@naver.com", "1234", "1111",
                "수정 테스트");

        //when

        //then
        assertThatThrownBy(() -> memberService.updateUser(request))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessageContaining("멤버 엔티티를 찾을 수 없습니다.");
    }

    @Test
    void 회원정보_수정_성공() throws Exception {
        // given
        memberRepository.save(Member.create("test", "test@naver.com", "1234"));
        MemberUpdateRequest request = new MemberUpdateRequest("test@naver.com", "1234", "1111",
                "수정 테스트");

        //when
        MemberUpdateResponse response = memberService.updateUser(request);

        //then
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("수정 테스트");
        assertThat(response.email()).isEqualTo("test@naver.com");
    }

}
