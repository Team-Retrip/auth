package com.retrip.auth.domain.entity;

import com.retrip.auth.domain.vo.MemberEmail;
import com.retrip.auth.domain.vo.MemberName;
import com.retrip.auth.domain.vo.MemberPassword;
import jakarta.persistence.*;

import java.util.UUID;

import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Getter
public class Member {
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


    public void matchPassword(PasswordEncoder passwordEncoder, String password) {
        this.password.matches(passwordEncoder, password);
    }


    public static Member create(String name, String email, String password) {
        return Member.builder()
                .id(UUID.randomUUID())
                .name(new MemberName(name))
                .email(new MemberEmail(email))
                .password(new MemberPassword(password))
                .build();
    }
}
