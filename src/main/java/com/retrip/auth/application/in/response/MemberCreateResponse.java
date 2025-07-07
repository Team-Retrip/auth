package com.retrip.auth.application.in.response;

import com.retrip.auth.domain.entity.Member;

import java.util.UUID;

public record MemberCreateResponse(
        UUID id,
        String email,
        String name
) {
    public static MemberCreateResponse of(Member member) {
        return new MemberCreateResponse(member.getId(), member.getEmail().getValue(), member.getName().getValue());
    }
}
