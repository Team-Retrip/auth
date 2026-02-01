package com.retrip.auth.application.in.response;

import com.retrip.auth.domain.entity.Member;
import java.util.UUID;

public record MemberCreateResponse(
        UUID id,
        String email,
        String name,
        String accessToken,
        String refreshToken
) {
    public static MemberCreateResponse of(Member member) {
        return new MemberCreateResponse(
                member.getId(),
                member.getEmail().getValue(),
                member.getName().getValue(),
                null,
                null
        );
    }

    public static MemberCreateResponse of(Member member, String accessToken, String refreshToken) {
        return new MemberCreateResponse(
                member.getId(),
                member.getEmail().getValue(),
                member.getName().getValue(),
                accessToken,
                refreshToken
        );
    }
}