package com.retrip.auth.domain.entity;

import com.retrip.auth.domain.vo.MemberEmail;
import com.retrip.auth.domain.vo.MemberName;
import com.retrip.auth.domain.vo.MemberPassword;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Getter
public class Member extends BaseEntity {
    @Id
    @Column(columnDefinition = "varbinary(16)")
    private UUID id;

    @Version
    private long version;

    @Embedded
    private MemberName name;

    @Embedded
    private MemberEmail email;

    @Embedded
    private MemberPassword password;

    @Embedded
    private Authorities authorities;

    private Boolean isDeleted;

    @Column(length = 20)
    private String provider;

    @Column(unique = true)
    private String providerId;

    // 본인인증 관련
    @Column(length = 88, unique = true)
    private String ci;

    @Column(length = 64)
    private String di;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    private LocalDateTime verifiedAt;

    // 기본 정보
    @Column(name = "gender", length = 1)
    private String gender;

    @Column(length = 10)
    private String birthDate;

    // 약관 동의
    @Column(nullable = false)
    @Builder.Default
    private boolean termsAgreed = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean marketingAgreed = false;

    // 프로필 관련
    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 30)
    private String bio;

    @Column(length = 4)
    private String mbti;

    // 설정
    @Column(nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    public String getPasswordValue() {
        return this.password != null ? this.password.getValue() : null;
    }

    public String getEmailValue() {
        return this.email != null ? this.email.getValue() : null;
    }

    public String getNameValue() {
        return this.name != null ? this.name.getValue() : null;
    }

    public static Member create(String name, String email, String password, List<String> authorities, String gender, String birthDate, boolean termsAgreed, boolean marketingAgreed) {
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .name(new MemberName(name))
                .email(new MemberEmail(email))
                .password(new MemberPassword(password))
                .isDeleted(false)
                .provider("local")
                .isVerified(false)
                .gender(gender)
                .birthDate(birthDate)
                .termsAgreed(termsAgreed)
                .marketingAgreed(marketingAgreed)
                .build();
        member.authorities = new Authorities(authorities, member);
        return member;
    }

    public static Member createSocialMember(String name, String email, String provider, String providerId) {
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .name(new MemberName(name))
                .email(new MemberEmail(email))
                .isDeleted(false)
                .password(new MemberPassword(null))
                .provider(provider)
                .providerId(providerId)
                .isVerified(false)
                .build();
        member.authorities = new Authorities(List.of("user"), member);
        return member;
    }

    public void update(String name, String password, String gender, String birthDate) {
        if (name != null) this.name = new MemberName(name);
        if (password != null) this.password = new MemberPassword(password);
        if (gender != null) this.gender = gender;
        if (birthDate != null) this.birthDate = birthDate;
    }

    public void updateProfile(String bio, String mbti, String profileImageUrl) {
        if (bio != null) this.bio = bio;
        if (mbti != null) this.mbti = mbti;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void updateNotificationSettings(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    public void updateIdentityVerification(String name, String gender, String birthDate, String ci, String di) {
        this.name = new MemberName(name);
        this.gender = gender;
        this.birthDate = birthDate;
        this.ci = ci;
        this.di = di;
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    public void updateSocialInfo(String name) {
        this.name = new MemberName(name);
    }

    public void updatePassword(String encodedPassword) {
        this.password = new MemberPassword(encodedPassword);
    }

    public void delete() {
        this.isDeleted = true;
    }
}
