package com.retrip.auth.application.in;

import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.request.MemberDeleteRequest;
import com.retrip.auth.application.in.request.MemberUpdateRequest;
import com.retrip.auth.application.in.response.MemberCreateResponse;
import com.retrip.auth.application.in.response.MemberUpdateResponse;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.vo.MemberEmail;
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
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
        boolean isDuplicate = !memberRepository.findByEmailAndIsDeletedFalse(new MemberEmail(request.email())).isEmpty();
        if (isDuplicate) {
            throw new DuplicateRequestException("Email already exists");
        }
        Member member = memberRepository.save(request.to(encode));
        return MemberCreateResponse.of(member);
    }

    @Override
    public MemberUpdateResponse updateUser(MemberUpdateRequest request) {
        String encodeNewPassword = passwordEncoder.encode(request.newPassword());

        Member member = findByEmail(request.email());
        if (isSamePassword(member.getPassword().getValue(), request.password())) { //여기에 패스워드 검증 로직이 들어가는게 맞는지..?
            member.update(encodeNewPassword, request.name());
        }
        return MemberUpdateResponse.of(member);
    }

    @Override
    public void deleteUser(MemberDeleteRequest request) {
        Member member = findByEmail(request.email());
        if (isSamePassword(member.getPassword().getValue(), request.password())) { //여기에 패스워드 검증 로직이 들어가는게 맞는지..?
            member.delete();
        }
    }


    private Member findByEmail(String email) {
        return memberRepository.findByEmailAndIsDeletedFalse(new MemberEmail(email))
                .stream().findAny().orElseThrow(MemberNotFoundException::new);
    }

    private boolean isSamePassword(String password, String inputPassword) {
        if (!passwordEncoder.matches(inputPassword, password)) {
            throw new BadCredentialsException("Bad credentials");
        }
        return true;
    }

}
