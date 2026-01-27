package com.retrip.auth.domain.entity;

import com.retrip.auth.domain.vo.AuthorityGrant;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.jaas.AuthorityGranter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Authority extends BaseEntity {
    @Id
    @Column(columnDefinition = "varbinary(16)")
    private UUID id;

    @Column(name = "is_granted", length = 50, nullable = false)
    private AuthorityGrant grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "member_id",
            nullable = false,
            columnDefinition = "varbinary(16)",
            foreignKey = @ForeignKey(name = "fk_authority_to_member")
    )
    private Member member;

    private Authority(String grant, Member member) {
        this.id = UUID.randomUUID();
        this.grant = AuthorityGrant.codeOf(grant);
        this.member = member;
    }

    public static Authority create(String grant, Member member) {
        return new Authority(grant, member);
    }
}
