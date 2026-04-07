package com.retrip.auth.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @Column(length = 36)
    private String token;

    @Column(nullable = false, length = 36)
    private String memberId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PasswordResetToken create(String memberId, int expireMinutes) {
        PasswordResetToken t = new PasswordResetToken();
        t.token = UUID.randomUUID().toString();
        t.memberId = memberId;
        t.expiresAt = LocalDateTime.now().plusMinutes(expireMinutes);
        t.used = false;
        t.createdAt = LocalDateTime.now();
        return t;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAsUsed() {
        this.used = true;
    }
}
