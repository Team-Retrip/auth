package com.retrip.auth.application.in.response;

import com.retrip.auth.domain.entity.Member;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberInfoResponse(
        UUID id,
        String email,
        String name,
        String nickname,
        boolean isVerified,
        boolean hasPassword,
        String lastLoginProvider,
        LocalDateTime createdAt
) {
    public static MemberInfoResponse of(Member member) {
        return new MemberInfoResponse(
                member.getId(),
                member.getEmailValue(),
                member.getNameValue(),
                member.getNickname(),
                member.isVerified(),
                member.hasPassword(),
                member.getProvider(),
                member.getCreatedAt()
        );
    }
}
