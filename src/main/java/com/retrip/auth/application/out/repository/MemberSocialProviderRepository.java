package com.retrip.auth.application.out.repository;

import com.retrip.auth.domain.entity.MemberSocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberSocialProviderRepository extends JpaRepository<MemberSocialProvider, Long> {
    List<MemberSocialProvider> findByMemberId(UUID memberId);
    Optional<MemberSocialProvider> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByMemberIdAndProvider(UUID memberId, String provider);
}
