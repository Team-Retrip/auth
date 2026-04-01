package com.retrip.auth.application.service;

import com.retrip.auth.application.dto.request.UpdateNotificationRequest;
import com.retrip.auth.application.dto.request.UpdateProfileRequest;
import com.retrip.auth.application.dto.response.ProfileResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.MemberSocialProviderRepository;
import com.retrip.auth.application.out.repository.MemberTravelStyleRepository;
import com.retrip.auth.application.out.repository.TravelStyleRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.MemberSocialProvider;
import com.retrip.auth.domain.entity.MemberTravelStyle;
import com.retrip.auth.domain.entity.TravelStyle;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.exception.common.InvalidValueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final MemberRepository memberRepository;
    private final TravelStyleRepository travelStyleRepository;
    private final MemberTravelStyleRepository memberTravelStyleRepository;
    private final MemberSocialProviderRepository socialProviderRepository;

    public ProfileResponse getProfile(String memberId) {
        Member member = memberRepository.findById(UUID.fromString(memberId))
                .orElseThrow(MemberNotFoundException::new);

        List<String> travelStyles = memberTravelStyleRepository.findByMemberId(member.getId())
                .stream()
                .map(mts -> mts.getTravelStyle().getName())
                .collect(Collectors.toList());

        List<MemberSocialProvider> socialProviders = socialProviderRepository.findByMemberId(member.getId());

        return ProfileResponse.from(member, travelStyles, socialProviders);
    }

    @Transactional
    public ProfileResponse updateProfile(String memberId, UpdateProfileRequest request) {
        Member member = memberRepository.findById(UUID.fromString(memberId))
                .orElseThrow(MemberNotFoundException::new);

        if (request.getNickname() != null) member.updateNickname(request.getNickname());
        member.updateProfile(request.getBio(), request.getMbti(), request.getProfileImageUrl());

        if (request.getTravelStyles() != null) {
            updateTravelStyles(member, request.getTravelStyles());
        }

        List<String> travelStyles = memberTravelStyleRepository.findByMemberId(member.getId())
                .stream()
                .map(mts -> mts.getTravelStyle().getName())
                .collect(Collectors.toList());

        List<MemberSocialProvider> socialProviders = socialProviderRepository.findByMemberId(member.getId());

        return ProfileResponse.from(member, travelStyles, socialProviders);
    }

    @Transactional
    public void updateNotificationSettings(String memberId, UpdateNotificationRequest request) {
        Member member = memberRepository.findById(UUID.fromString(memberId))
                .orElseThrow(MemberNotFoundException::new);
        member.updateNotificationSettings(request.getEnabled());
    }

    private void updateTravelStyles(Member member, List<String> styleNames) {
        memberTravelStyleRepository.deleteByMemberId(member.getId());
        for (String styleName : styleNames) {
            TravelStyle travelStyle = travelStyleRepository.findByName(styleName)
                    .orElseThrow(() -> new InvalidValueException("존재하지 않는 여행 스타일입니다: " + styleName));
            MemberTravelStyle memberTravelStyle = MemberTravelStyle.of(member, travelStyle);
            memberTravelStyleRepository.save(memberTravelStyle);
        }
    }
}
