package com.retrip.auth.application.in.response;

import com.retrip.auth.domain.entity.Member;

import java.util.UUID;

public record MemberSearchResponse(
        UUID id,
        String name,
        String profileImageUrl,
        String bio
) {
    public static MemberSearchResponse of(Member member) {
        return new MemberSearchResponse(
                member.getId(),
                member.getNameValue(),
                member.getProfileImageUrl(),
                member.getBio()
        );
    }
}
