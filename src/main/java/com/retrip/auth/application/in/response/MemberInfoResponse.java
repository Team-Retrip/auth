
package com.retrip.auth.application.in.response;

import com.retrip.auth.domain.entity.Member;
import java.time.LocalDateTime;
import java.util.UUID;

public record MemberInfoResponse(
        UUID id,
        String email,
        String name,
        LocalDateTime createdAt
) {
    public static MemberInfoResponse of(Member member) {
        return new MemberInfoResponse(
                member.getId(),
                member.getEmail().getValue(),
                member.getName().getValue(),
                member.getCreatedAt()
        );
    }
}