package com.retrip.auth.application.in;

import com.retrip.auth.domain.entity.Member;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

@Getter
@Builder
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String provider;
    private String providerId;

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .provider("google")
                .providerId((String) attributes.get(userNameAttributeName))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) throw new OAuth2AuthenticationException("카카오 계정 정보를 가져올 수 없습니다.");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) throw new OAuth2AuthenticationException("카카오 프로필 정보를 가져올 수 없습니다.");

        String email = (String) kakaoAccount.get("email");
        if (email == null) {
            // 비즈앱 미인증 시 이메일 제공 불가 → providerId 기반 placeholder 사용
            // 계정 연동 테스트 시: 이 이메일로 일반 회원가입 후 카카오 로그인하면 연동됨
            email = "kakao_" + attributes.get(userNameAttributeName) + "@kakao.social";
        }

        return OAuthAttributes.builder()
                .name((String) profile.get("nickname"))
                .email(email)
                .provider("kakao")
                .providerId(String.valueOf(attributes.get(userNameAttributeName)))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // 네이버 응답은 'response' 키 안에 실제 사용자 정보가 중첩되어 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get(userNameAttributeName);

        return OAuthAttributes.builder()
                .name((String) response.get("name")) // 네이버는 'name' 필드를 제공 (WBS 스코프에 nickname이 아닌 name으로 되어있어 name 사용)
                .email((String) response.get("email"))
                .provider("naver")
                .providerId((String) response.get("id"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }


    public Member toEntity() {
        return Member.createSocialMember(name, email, provider);
    }
}