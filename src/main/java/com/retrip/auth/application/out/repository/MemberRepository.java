package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.Member;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailValue(String email);
}
