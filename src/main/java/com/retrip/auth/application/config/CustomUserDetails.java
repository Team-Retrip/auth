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
public class CustomUserDetails implements UserDetails, OAuth2User { // [수정] OAuth2User 인터페이스 추가

    private final Member member;
    private Map<String, Object> attributes; // [신규] OAuth2 제공자로부터 받은 원본 데이터

    // 일반 로그인용 생성자
    public CustomUserDetails(Member member) {
        this.member = member;
    }

    // [신규] OAuth2 로그인용 생성자
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
        // 소셜 로그인 회원은 비밀번호가 null일 수 있음
        return member.getPassword().getValue();
    }

    @Override
    public String getUsername() {
        return member.getEmail().getValue();
    }

    // [신규] OAuth2User 인터페이스 메서드 구현
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return member.getId().toString();
    }
}