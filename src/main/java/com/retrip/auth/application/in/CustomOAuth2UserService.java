package com.retrip.auth.application.in;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.vo.MemberEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate; // ★ import 추가
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
        // 1. 소셜 서비스에서 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        // 2. 데이터 파싱
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 3. 회원 저장/업데이트
        Member member = saveOrUpdate(attributes);

        // ★ [핵심 수정] 지연 로딩 문제 해결 ★
        // 트랜잭션이 끝나기 전에 권한 목록을 미리 읽어서 메모리에 올려둡니다.
        Hibernate.initialize(member.getAuthorities().getValues());

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