package com.retrip.auth.application.in.factory;

import com.retrip.auth.application.in.MemberService;
import com.retrip.auth.application.out.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Transactional
public class BaseMemberServiceTest {
    @Autowired
    protected MemberService memberService;

    @Autowired
    protected MemberRepository memberRepository;


    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
    }
}
