package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.Member;

import com.retrip.auth.domain.vo.MemberEmail;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByEmail(MemberEmail email);
}
