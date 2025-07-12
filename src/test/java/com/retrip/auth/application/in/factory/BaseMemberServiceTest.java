package com.retrip.auth.application.in.factory;

import com.retrip.auth.application.in.MemberService;
import com.retrip.auth.application.out.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
public class BaseMemberServiceTest {
    @Autowired
    protected MemberService memberService;

    @Autowired
    protected MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
    }
}
