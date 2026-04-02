package com.retrip.auth.application.in;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.MemberSocialProviderRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.MemberSocialProvider;
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
    private final MemberSocialProviderRepository socialProviderRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        Member member = saveOrUpdate(attributes);

        // Hibernate 세션이 닫히기 전에 authorities 강제 초기화 (LazyInitializationException 방지)
        member.getAuthorities().getValues().size();

        return new CustomUserDetails(member, oAuth2User.getAttributes());
    }

    private Member saveOrUpdate(OAuthAttributes attributes) {

        // Step 1: providerId로 기존 연동 확인 (재로그인)
        Optional<MemberSocialProvider> existingSocial =
                socialProviderRepository.findByProviderAndProviderId(
                        attributes.getProvider(), attributes.getProviderId());

        if (existingSocial.isPresent()) {
            Member member = memberRepository.findById(existingSocial.get().getMemberId())
                    .orElseThrow(() -> new OAuth2AuthenticationException("연동된 멤버를 찾을 수 없습니다."));
            member.updateLastLoginProvider(attributes.getProvider());
            log.info("소셜 재로그인: {} ({})", attributes.getEmail(), attributes.getProvider());
            return member;
        }

        // Step 2: 이메일로 기존 계정 확인 (다른 소셜 or 이메일 계정 → 자동 연동)
        // placeholder 이메일(kakao_xxx@kakao.social)은 실제 이메일이 아니므로 연동 스킵
        boolean isPlaceholderEmail = attributes.getEmail() != null
                && attributes.getEmail().endsWith("@kakao.social");

        Optional<Member> existingMember = isPlaceholderEmail
                ? Optional.empty()
                : memberRepository.findByEmailAndIsDeletedFalse(new MemberEmail(attributes.getEmail()))
                        .stream().findFirst();

        if (existingMember.isPresent()) {
            Member member = existingMember.get();
            if (!socialProviderRepository.existsByMemberIdAndProvider(member.getId(), attributes.getProvider())) {
                socialProviderRepository.save(
                        MemberSocialProvider.create(member.getId(), attributes.getProvider(),
                                attributes.getProviderId(), attributes.getEmail()));
            }
            member.updateLastLoginProvider(attributes.getProvider());
            log.info("소셜 계정 연동: {} → {}", attributes.getEmail(), attributes.getProvider());
            return member;
        }

        // Step 3: 신규 가입
        Member newMember = attributes.toEntity();
        memberRepository.save(newMember);
        socialProviderRepository.save(
                MemberSocialProvider.create(newMember.getId(), attributes.getProvider(),
                        attributes.getProviderId(), attributes.getEmail()));
        log.info("신규 소셜 회원 가입: {} ({})", attributes.getEmail(), attributes.getProvider());
        return newMember;
    }
}
