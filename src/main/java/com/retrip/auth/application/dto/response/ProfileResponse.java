package com.retrip.auth.application.dto.response;

import com.retrip.auth.domain.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProfileResponse {
    private String email;
    private String name;
    private String gender;
    private String birthDate;
    private Boolean isVerified;
    private String profileImageUrl;
    private String bio;
    private String mbti;
    private List<String> travelStyles;
    private Boolean notificationEnabled;

    public static ProfileResponse from(Member member, List<String> travelStyles) {
        return ProfileResponse.builder()
                .email(member.getEmailValue())
                .name(member.getNameValue())
                .gender(member.getGender())
                .birthDate(member.getBirthDate())
                .isVerified(member.isVerified())
                .profileImageUrl(member.getProfileImageUrl())
                .bio(member.getBio())
                .mbti(member.getMbti())
                .travelStyles(travelStyles)
                .notificationEnabled(member.isNotificationEnabled())
                .build();
    }
}
