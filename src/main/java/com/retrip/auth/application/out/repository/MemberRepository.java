package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.vo.MemberEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    List<Member> findByEmailAndIsDeletedFalse(MemberEmail email);
    List<Member> findByEmail(MemberEmail email);

    // 추가: 이메일로 단건 조회 (Optional)
    Optional<Member> findByEmail_Value(String email);

    // 추가: CI 중복 체크
    boolean existsByCi(String ci);

    @Query("SELECT m FROM Member m WHERE LOWER(m.name.value) LIKE LOWER(CONCAT('%', :name, '%')) AND m.isDeleted = false")
    List<Member> searchByNameContainingIgnoreCase(@Param("name") String name);

    List<Member> findAllByIdInAndIsDeletedFalse(List<UUID> ids);
}
