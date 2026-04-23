package com.retrip.auth.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verification_codes",
        indexes = @Index(name = "idx_evc_email", columnList = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationCode {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static EmailVerificationCode create(String email, String code, int expireMinutes) {
        EmailVerificationCode e = new EmailVerificationCode();
        e.id = UUID.randomUUID().toString();
        e.email = email;
        e.code = code;
        e.expiresAt = LocalDateTime.now().plusMinutes(expireMinutes);
        e.verified = false;
        e.createdAt = LocalDateTime.now();
        return e;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isVerifiedWithin(int minutes) {
        return verified && verifiedAt != null
                && verifiedAt.isAfter(LocalDateTime.now().minusMinutes(minutes));
    }

    public void markVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }
}
