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


    public CustomUserDetails(Member member) {
        this.member = member;
    }


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

        return member.getPassword().getValue();
    }

    @Override
    public String getUsername() {
        return member.getEmail().getValue();
    }


    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return member.getId().toString();
    }
}