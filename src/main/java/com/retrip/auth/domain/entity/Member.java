package com.retrip.auth.domain.entity;

import com.retrip.auth.domain.vo.MemberEmail;
import com.retrip.auth.domain.vo.MemberName;
import com.retrip.auth.domain.vo.MemberPassword;
import jakarta.persistence.*;
import lombok.*;

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

    // [신규 추가] 소셜 로그인 정보 및 본인인증 정보 필드
    @Column(length = 20)
    private String provider; // e.g., "local", "kakao", "google"

    @Column(unique = true)
    private String providerId; // 소셜 플랫폼의 고유 사용자 ID

    @Column(length = 88, unique = true)
    private String ci; // 본인인증 연계정보 (CI)

    @Column(nullable = false)
    private boolean isVerified = false; // 본인인증 여부

    public static Member create(String name, String email, String password, List<String> authorities) {
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .name(new MemberName(name))
                .email(new MemberEmail(email))
                .password(new MemberPassword(password))
                .isDeleted(false)
                .provider("local") // [신규] 기본값 설정
                .isVerified(false) // [신규] 기본값 설정
                .build();
        member.authorities = new Authorities(authorities, member);
        return member;
    }

    public static Member create(String name, String email, String password) {
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .name(new MemberName(name))
                .email(new MemberEmail(email))
                .isDeleted(false)
                .password(new MemberPassword(password))
                .provider("local") // [신규] 기본값 설정
                .isVerified(false) // [신규] 기본값 설정
                .build();
        member.authorities = new Authorities(List.of("user"), member);
        return member;
    }

    // [신규 추가] 소셜 로그인 회원 생성 메서드
    public static Member createSocialMember(String name, String email, String provider, String providerId) {
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .name(new MemberName(name))
                .email(new MemberEmail(email))
                .isDeleted(false)
                .password(new MemberPassword(null)) // 비밀번호 없음
                .provider(provider)
                .providerId(providerId)
                .isVerified(false)
                .build();
        member.authorities = new Authorities(List.of("user"), member);
        return member;
    }

    public void update(String password, String name) {
        this.password = new MemberPassword(password);
        this.name = new MemberName(name);
    }

    // [신규 추가] 소셜 회원 정보 업데이트 메서드
    public void updateSocialInfo(String name) {
        this.name = new MemberName(name);
    }

    public void delete() {
        this.isDeleted = true;
    }
}