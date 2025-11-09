package com.retrip.auth.application.in;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.vo.MemberEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        Member member = saveOrUpdate(attributes);

        return new CustomUserDetails(member, oAuth2User.getAttributes());
    }

    private Member saveOrUpdate(OAuthAttributes attributes) {
        Optional<Member> memberOptional = memberRepository.findByEmailAndIsDeletedFalse(new MemberEmail(attributes.getEmail()))
                .stream().findFirst();

        Member member;
        if (memberOptional.isPresent()) {
            member = memberOptional.get();
            if (member.getProvider() == null || member.getProvider().equals("local")) {
                log.warn("기존 로컬 계정({})으로 소셜 로그인을 시도했습니다.", attributes.getEmail());
                // TODO: 추후 계정 연동 정책에 따라 로직 수정 필요 (현재는 정보 업데이트만 수행)
            }
            member.updateSocialInfo(attributes.getName());
        } else {
            member = attributes.toEntity();
            memberRepository.save(member);
            log.info("신규 소셜 회원 가입: {} ({})", attributes.getEmail(), attributes.getProvider());
        }
        return member;
    }
}