package com.retrip.auth.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member_social_provider",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_provider", columnNames = {"member_id", "provider"}),
                @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "provider_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSocialProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, columnDefinition = "varbinary(16)")
    private UUID memberId;

    @Column(length = 20, nullable = false)
    private String provider;        // "kakao" | "naver" | "google"

    @Column(name = "provider_id", length = 255, nullable = false)
    private String providerId;      // 소셜 서비스의 유저 고유 ID

    @Column(length = 50)
    private String email;           // 해당 소셜 계정의 이메일

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    public static MemberSocialProvider create(UUID memberId, String provider, String providerId, String email) {
        MemberSocialProvider msp = new MemberSocialProvider();
        msp.memberId = memberId;
        msp.provider = provider;
        msp.providerId = providerId;
        msp.email = email;
        msp.linkedAt = LocalDateTime.now();
        return msp;
    }
}
