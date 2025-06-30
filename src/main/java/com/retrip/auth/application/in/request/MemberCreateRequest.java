package com.retrip.auth.application.in.request;

import com.retrip.auth.domain.entity.Member;

public record MemberCreateRequest(
        String email,
        String password,
        String name
) {
    public Member to(String encodePassword) {
        return Member.create(name, email, encodePassword);
    }
}
