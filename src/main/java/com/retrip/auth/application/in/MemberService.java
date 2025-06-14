package com.retrip.auth.application.in;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import com.retrip.auth.application.out.repository.MemberQueryRepository;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService implements ManageMemberUseCase, UserDetailsService {

    private final MemberQueryRepository memberQueryRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberQueryRepository.findByEmailWithAuthorities(username).orElseThrow(MemberNotFoundException::new);
        return new CustomUserDetails(member);
    }
}
