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

    @Column(length = 20)
    private String provider;

    @Column(unique = true)
    private String providerId;

    @Column(length = 88, unique = true)
    private String ci;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "gender")
    private String gender;

    @Column(name = "age")
    private Integer age;

    public String getPasswordValue() {
        return this.password != null ? this.password.getValue() : null;
    }

    public String getEmailValue() {
        return this.email != null ? this.email.getValue() : null;
    }

    public String getNameValue() {
        return this.name != null ? this.name.getValue() : null;
    }

    public static Member create(String name, String email, String password, List<String> authorities, String gender, Integer age) {
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .name(new MemberName(name))
                .email(new MemberEmail(email))
                .password(new MemberPassword(password))
                .isDeleted(false)
                .provider("local")
                .isVerified(false)
                .gender(gender)
                .age(age)
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

    public void update(String name, String password, String gender, Integer age) {
        if (name != null) this.name = new MemberName(name);
        if (password != null) this.password = new MemberPassword(password);
        if (gender != null) this.gender = gender;
        if (age != null) this.age = age;
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