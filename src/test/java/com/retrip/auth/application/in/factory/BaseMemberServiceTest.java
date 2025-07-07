package com.retrip.auth.application.in.factory;

import com.retrip.auth.application.in.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BaseMemberServiceTest {
    @Autowired
    protected MemberService memberService;



    @BeforeEach
    void setUp() {
    }
}
