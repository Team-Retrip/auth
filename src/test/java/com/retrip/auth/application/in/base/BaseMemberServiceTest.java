package com.retrip.auth.application.in.base;

import com.retrip.auth.application.in.MemberService;
import com.retrip.auth.application.out.repository.MemberRepository;

import com.retrip.auth.domain.entity.Member;


import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public abstract class BaseMemberServiceTest {
    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;


    protected MemberService memberService;

    protected Member member;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, passwordEncoder);
        member = Member.create( "테스트", "test@naver.com", passwordEncoder.encode("1234"));
    }
}
