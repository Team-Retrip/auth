package com.retrip.auth.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CI 미매칭 사용자에 대한 PortOne 인증 세션.
 * 이름+생년월일로 후보를 찾은 뒤 이메일 OTP로 소유권 확인이 완료되면 CI를 연결한다.
 */
@Entity
@Table(name = "portone_verification_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortOneVerificationSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String memberId;

    @Column(length = 100)
    private String ci;

    @Column(length = 100)
    private String di;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 1)
    private String gender;

    @Column(length = 10)
    private String birthDate;

    /** "FIND_EMAIL" | "PASSWORD_RESET" */
    @Column(nullable = false, length = 20)
    private String purpose;

    @Column(length = 6)
    private String emailCode;

    private LocalDateTime emailCodeExpiresAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PortOneVerificationSession create(
            String memberId, String ci, String di,
            String name, String gender, String birthDate,
            String purpose, int sessionExpireMinutes) {
        PortOneVerificationSession s = new PortOneVerificationSession();
        s.id = UUID.randomUUID().toString();
        s.memberId = memberId;
        s.ci = ci;
        s.di = di;
        s.name = name;
        s.gender = gender;
        s.birthDate = birthDate;
        s.purpose = purpose;
        s.expiresAt = LocalDateTime.now().plusMinutes(sessionExpireMinutes);
        s.used = false;
        s.createdAt = LocalDateTime.now();
        return s;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isEmailCodeExpired() {
        return emailCodeExpiresAt == null || LocalDateTime.now().isAfter(emailCodeExpiresAt);
    }

    public void setEmailCode(String code, int expireMinutes) {
        this.emailCode = code;
        this.emailCodeExpiresAt = LocalDateTime.now().plusMinutes(expireMinutes);
    }

    public void markUsed() {
        this.used = true;
    }
}
