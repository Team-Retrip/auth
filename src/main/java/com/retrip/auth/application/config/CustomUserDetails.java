package com.retrip.auth.application.config;

import com.retrip.auth.domain.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Member member;
    private Map<String, Object> attributes;

    // 일반 로그인용 생성자
    public CustomUserDetails(Member member) {
        this.member = member;
    }

    // OAuth2 로그인용 생성자
    public CustomUserDetails(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getAuthorities().getValues().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getGrant().getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return member.getPassword() != null ? member.getPassword().getValue() : null;
    }

    @Override
    public String getUsername() {
        // UserDetails의 식별자는 PK(UUID)를 문자열로 반환
        return member.getId().toString();
    }

    // =============================================================
    // ★ [추가] JwtProvider에서 Claims 생성 시 사용하기 위한 편의 메서드들
    // =============================================================
    public String getEmail() {
        return member.getEmail().getValue();
    }

    public String getRealName() {
        // OAuth2User의 getName()과 구분하기 위해 이름을 다르게 설정
        return member.getName().getValue();
    }

    public String getGender() {
        return member.getGender();
    }

    public String getBirthDate() {
        return member.getBirthDate();
    }

    public String getNickname() {
        return member.getNickname();
    }
    // =============================================================

    // OAuth2User 메서드
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        // OAuth2User의 식별자도 PK(UUID)로 통일
        return member.getId().toString();
    }

    // UserDetails 필수 메서드들 (계정 상태 확인 - 모두 true 반환)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
