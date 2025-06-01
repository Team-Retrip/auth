package com.retrip.auth.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Embeddable
@NoArgsConstructor(access = PROTECTED, force = true)
public class Authorities {
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Authority> values = new ArrayList<>();

    public Authorities(List<String> authorities, Member member) {
        validate(authorities);
        authorities.forEach(authority -> this.values.add(Authority.create(authority, member)));
    }

    private void validate(List<String> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
    }
}
