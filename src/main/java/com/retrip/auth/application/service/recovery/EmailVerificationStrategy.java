package com.retrip.auth.application.service.recovery;

import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationStrategy {

    private final MemberRepository memberRepository;

    /**
     * 이메일로 회원을 조회한다.
     * 비밀번호가 없는 소셜 계정도 이메일 인증을 통해 처음으로 비밀번호를 설정할 수 있다.
     */
    public Member findMember(String email) {
        Member member = memberRepository.findByEmail_Value(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (Boolean.TRUE.equals(member.getIsDeleted())) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }
}
