package com.retrip.auth.infra.adapter.in.rest.filter.base;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.retrip.auth.application.in.MemberQueryService;
import com.retrip.auth.application.out.repository.MemberQueryRepository;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.infra.adapter.out.persistence.mysql.query.MemberQuerydslRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootTest
public abstract class BaseLoginAuthenticationTest {
    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected JPAQueryFactory jpaQueryFactory;
    protected MemberQueryRepository memberQueryRepository;
    protected MemberQueryService memberQueryService;

    protected Member member;

    @BeforeEach
    void setUp() {
        memberQueryRepository = new MemberQuerydslRepository(jpaQueryFactory);
        memberQueryService = new MemberQueryService(memberQueryRepository);
        member = Member.create(
                "테스트",
                "test@naver.com",
                passwordEncoder.encode("1234"),
                List.of("admin"),
                null,
                null,
                true,
                false,
                null
        );
    }
}
