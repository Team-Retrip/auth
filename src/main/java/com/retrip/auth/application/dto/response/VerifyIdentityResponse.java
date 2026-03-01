package com.retrip.auth.application.dto.response;

import com.retrip.auth.domain.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerifyIdentityResponse {
    private String name;
    private String gender;
    private String birthDate;
    private Boolean isVerified;

    public static VerifyIdentityResponse from(Member member) {
        return VerifyIdentityResponse.builder()
                .name(member.getNameValue())
                .gender(member.getGender())
                .birthDate(member.getBirthDate())
                .isVerified(member.isVerified())
                .build();
    }
}
