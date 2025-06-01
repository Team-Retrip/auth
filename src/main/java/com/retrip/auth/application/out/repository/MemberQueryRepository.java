package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.Member;

import java.util.Optional;
import java.util.UUID;

public interface MemberQueryRepository {
    Optional<Member> findByEmailWithAuthorities(String email);
}
