
package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.vo.MemberEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    List<Member> findByEmailAndIsDeletedFalse(MemberEmail email);
    List<Member> findByEmail(MemberEmail email);
}