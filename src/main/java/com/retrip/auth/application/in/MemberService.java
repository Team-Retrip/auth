package com.retrip.auth.application.in;

import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
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
    public MemberCreateResponse createUser(MemberCreateRequest request) {
        String encode = passwordEncoder.encode(request.password());
        Member member = memberRepository.save(request.to(encode));
        return MemberCreateResponse.of(member);
    }
}
