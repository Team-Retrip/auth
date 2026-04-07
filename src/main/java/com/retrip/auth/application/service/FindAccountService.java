package com.retrip.auth.application.service;

import com.retrip.auth.application.dto.response.FindEmailResponse;
import com.retrip.auth.application.dto.response.PasswordResetTokenResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.PasswordResetTokenRepository;
import com.retrip.auth.application.service.recovery.EmailVerificationStrategy;
import com.retrip.auth.application.service.recovery.PortOneVerificationStrategy;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.PasswordResetToken;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FindAccountService {

    private final PortOneVerificationStrategy portOneStrategy;
    private final EmailVerificationStrategy emailStrategy;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.expire-minutes:30}")
    private int expireMinutes;

    /** 아이디 찾기 (본인인증) */
    public FindEmailResponse findEmailByVerification(String impUid) {
        Member member = portOneStrategy.findMember(impUid);
        return FindEmailResponse.of(member.getEmailValue(), member.isVerified());
    }

    /** 비밀번호 재설정 토큰 발급 (본인인증) */
    public PasswordResetTokenResponse issueResetTokenByVerification(String impUid) {
        Member member = portOneStrategy.findMember(impUid);
        if (!member.hasPassword()) {
            throw new BusinessException(ErrorCode.SOCIAL_MEMBER_NO_PASSWORD_RESET);
        }
        return new PasswordResetTokenResponse(createToken(member).getToken());
    }

    /** 비밀번호 재설정 이메일 발송 (이메일 경로) */
    public void sendPasswordResetEmail(String email) {
        Member member = emailStrategy.findMember(email);
        String token = createToken(member).getToken();
        emailService.sendPasswordResetEmail(member.getEmailValue(), token);
    }

    /** 비밀번호 재설정 (토큰 검증 + 새 비밀번호 저장) */
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = resetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_NOT_FOUND));

        if (token.isUsed()) throw new BusinessException(ErrorCode.RESET_TOKEN_ALREADY_USED);
        if (token.isExpired()) throw new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED);

        Member member = memberRepository.findById(UUID.fromString(token.getMemberId()))
                .orElseThrow(MemberNotFoundException::new);

        member.updatePassword(passwordEncoder.encode(newPassword));
        token.markAsUsed();
    }

    private PasswordResetToken createToken(Member member) {
        resetTokenRepository.deleteByMemberId(member.getId().toString());
        return resetTokenRepository.save(PasswordResetToken.create(member.getId().toString(), expireMinutes));
    }
}
