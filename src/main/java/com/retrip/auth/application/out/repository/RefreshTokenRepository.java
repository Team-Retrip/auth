package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenValue(String tokenValue);
    Optional<RefreshToken> findByMemberId(String memberId);

    void deleteByMemberId(String memberId);
}