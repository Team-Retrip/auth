package com.retrip.auth.application.in;

import com.retrip.auth.application.in.request.LoginRequest;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService implements ManageMemberUseCase {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void login(LoginRequest request) {
        Member member = memberRepository.findByEmailValue(request.email()).orElseThrow(MemberNotFoundException::new);
        member.matchPassword(passwordEncoder, request.password());
    }
}
