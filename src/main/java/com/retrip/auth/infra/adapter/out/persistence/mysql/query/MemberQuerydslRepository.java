package com.retrip.auth.infra.adapter.out.persistence.mysql.query;

import static com.retrip.auth.domain.entity.QAuthority.authority;
import static com.retrip.auth.domain.entity.QMember.member;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.retrip.auth.application.out.repository.MemberQueryRepository;
import com.retrip.auth.domain.entity.Member;

import com.retrip.auth.domain.entity.QAuthority;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class MemberQuerydslRepository implements MemberQueryRepository {
    private final JPAQueryFactory query;

    @Override
    public Optional<Member> findByEmailWithAuthorities(String email) {
        return Optional.ofNullable(query.selectFrom(member)
                .leftJoin(member.authorities.values, authority)
                .fetchJoin()
                .where(member.email.value.eq(email), member.isDeleted.isFalse())
                .fetchOne());
    }
}
