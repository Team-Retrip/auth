package com.retrip.auth.application.config;

import com.retrip.auth.application.in.MemberQueryService;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.PasswordNotMatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    private final MemberQueryService memberQueryService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = (String) authentication.getCredentials();

        Member member = memberQueryService.getMemberByEmail(email);

        if (!passwordEncoder.matches(password, member.getPasswordValue())) {
            throw new PasswordNotMatchException();
        }

        // ★ 핵심: 여기서 CustomUserDetails를 만들어서 넣어줘야 JwtProvider가 정보를 꺼낼 수 있음
        CustomUserDetails userDetails = new CustomUserDetails(member);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthentication.class.isAssignableFrom(authentication);
    }
}
