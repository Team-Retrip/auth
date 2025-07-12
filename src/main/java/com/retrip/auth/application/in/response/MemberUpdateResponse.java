package com.retrip.auth.application.in.response;

import com.retrip.auth.domain.entity.Member;

import java.util.UUID;

public record MemberUpdateResponse(
        UUID id,
        String email,
        String name
) {
    public static MemberUpdateResponse of(Member member) {
        return new MemberUpdateResponse(member.getId(), member.getEmail().getValue(), member.getName().getValue());
    }
}
