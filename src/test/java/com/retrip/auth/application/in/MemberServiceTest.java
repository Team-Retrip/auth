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
import com.retrip.auth.domain.exception.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;
import java.util.UUID;

class MemberServiceTest extends BaseMemberServiceTest {

    @Test
    void 회원가입_성공() throws Exception {
        // given
        MemberCreateRequest request = new MemberCreateRequest("test@naver.com", "1234", "test", null, null, null, true, false);

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
        memberRepository.save(Member.create("test", "test@naver.com", passwordEncoder.encode("1234"), List.of("user"), null, null, true, false, null));
        MemberCreateRequest request = new MemberCreateRequest("test@naver.com", "1111", "중복 테스트", null, null, null, true, false);

        //when

        //then
        assertThatThrownBy(() -> memberService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는 이메일입니다.");
    }


    @Test
    void 회원정보_수정_패스워드_달라_실패() throws Exception {
        // given
        Member saved = memberRepository.save(Member.create("test", "test@naver.com", passwordEncoder.encode("1234"), List.of("user"), null, null, true, false, null));
        MemberUpdateRequest request = new MemberUpdateRequest("1235", "1111", "수정 테스트", null, null);

        //when

        //then
        assertThatThrownBy(() -> memberService.updateUser(saved.getId(), request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");
    }

    @Test
    void 저장된_회원_정보가_없어서_실패() throws Exception {
        // given
        MemberUpdateRequest request = new MemberUpdateRequest("1234", "1111", "수정 테스트", null, null);

        //when

        //then
        assertThatThrownBy(() -> memberService.updateUser(UUID.randomUUID(), request))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessageContaining("멤버 엔티티를 찾을 수 없습니다.");
    }

    @Test
    void 회원정보_수정_성공() throws Exception {
        // given
        Member saved = memberRepository.save(Member.create("test", "test@naver.com", passwordEncoder.encode("1234"), List.of("user"), null, null, true, false, null));
        MemberUpdateRequest request = new MemberUpdateRequest("1234", "1111", "수정 테스트", null, null);

        //when
        MemberUpdateResponse response = memberService.updateUser(saved.getId(), request);

        //then
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("수정 테스트");
        assertThat(response.email()).isEqualTo("test@naver.com");
    }

}
