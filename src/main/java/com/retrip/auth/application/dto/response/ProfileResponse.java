package com.retrip.auth.application.dto.response;

import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.MemberSocialProvider;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ProfileResponse {
    private String email;
    private String name;
    private String nickname;
    private String gender;
    private String birthDate;
    private Boolean isVerified;
    private String profileImageUrl;
    private String bio;
    private String mbti;
    private List<String> travelStyles;
    private Boolean notificationEnabled;
    private Boolean hasPassword;
    private String lastLoginProvider;
    private List<LinkedProviderInfo> linkedProviders;

    public record LinkedProviderInfo(String provider, LocalDateTime linkedAt) {}

    public static ProfileResponse from(Member member, List<String> travelStyles,
                                       List<MemberSocialProvider> socialProviders) {
        List<LinkedProviderInfo> linked = socialProviders.stream()
                .map(sp -> new LinkedProviderInfo(sp.getProvider(), sp.getLinkedAt()))
                .toList();

        return ProfileResponse.builder()
                .email(member.getEmailValue())
                .name(member.getNameValue())
                .nickname(member.getNickname())
                .gender(member.getGender())
                .birthDate(member.getBirthDate())
                .isVerified(member.isVerified())
                .profileImageUrl(member.getProfileImageUrl())
                .bio(member.getBio())
                .mbti(member.getMbti())
                .travelStyles(travelStyles)
                .notificationEnabled(member.isNotificationEnabled())
                .hasPassword(member.hasPassword())
                .lastLoginProvider(member.getProvider())
                .linkedProviders(linked)
                .build();
    }
}
