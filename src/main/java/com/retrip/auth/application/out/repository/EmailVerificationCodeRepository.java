package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, String> {

    Optional<EmailVerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);
}
