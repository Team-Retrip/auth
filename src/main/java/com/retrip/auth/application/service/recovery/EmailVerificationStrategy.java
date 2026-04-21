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
     * 이메일로 회원을 조회한다. 소셜 전용 계정(비밀번호 없음) 여부는 호출부에서 검사한다.
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
