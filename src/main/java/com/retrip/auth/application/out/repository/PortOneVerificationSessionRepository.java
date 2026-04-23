package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.PortOneVerificationSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortOneVerificationSessionRepository extends JpaRepository<PortOneVerificationSession, String> {
}
