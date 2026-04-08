package com.retrip.auth.application.service;

import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.dto.response.FindEmailResponse;
import com.retrip.auth.application.dto.response.PasswordResetTokenResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.PasswordResetTokenRepository;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.application.service.recovery.EmailVerificationStrategy;
import com.retrip.auth.application.service.recovery.PortOneVerificationStrategy;
import com.retrip.auth.application.service.recovery.VerificationResult;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.PasswordResetToken;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import com.retrip.auth.infra.adapter.out.external.PortOneApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindAccountService {

    private final PortOneApiClient portOneApiClient;
    private final PortOneVerificationStrategy portOneStrategy;
    private final EmailVerificationStrategy emailStrategy;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.password-reset.expire-minutes:30}")
    private int expireMinutes;

    /**
     * 아이디 찾기 (본인인증)
     * HTTP 호출 후 트랜잭션 시작 — DB 커넥션 점유 최소화
     */
    public FindEmailResponse findEmailByVerification(String impUid) {
        CertificationInfo certInfo = portOneApiClient.getCertificationInfo(impUid); // HTTP, 트랜잭션 밖
        VerificationResult result = portOneStrategy.findMemberByCert(certInfo);     // @Transactional
        return FindEmailResponse.of(result.member().getEmailValue(), result.wasJustVerified());
    }

    /**
     * 비밀번호 재설정 토큰 발급 (본인인증)
     * HTTP 호출 후 트랜잭션 시작 — DB 커넥션 점유 최소화.
     * TransactionTemplate으로 DB 작업만 트랜잭션에 포함시킨다.
     */
    public PasswordResetTokenResponse issueResetTokenByVerification(String impUid) {
        CertificationInfo certInfo = portOneApiClient.getCertificationInfo(impUid); // HTTP, 트랜잭션 밖
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            VerificationResult result = portOneStrategy.findMemberByCert(certInfo);
            Member member = result.member();
            if (!member.hasPassword()) {
                throw new BusinessException(ErrorCode.SOCIAL_MEMBER_NO_PASSWORD_RESET);
            }
            return new PasswordResetTokenResponse(createToken(member).getToken());
        });
    }

    /**
     * 비밀번호 재설정 이메일 발송 (이메일 경로)
     * TransactionTemplate으로 토큰 저장 트랜잭션을 먼저 커밋한 뒤 이메일 발송.
     * SMTP 호출이 DB 트랜잭션 안에 포함되지 않도록 경계를 분리한다.
     */
    public void sendPasswordResetEmail(String email) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String[] tokenData = tx.execute(status -> {
            Member member = emailStrategy.findMember(email);
            String token = createToken(member).getToken();
            return new String[]{member.getEmailValue(), token};
        });
        emailService.sendPasswordResetEmail(tokenData[0], tokenData[1]); // 트랜잭션 커밋 후 발송
    }

    /**
     * 비밀번호 재설정 (토큰 검증 + 새 비밀번호 저장)
     * 비밀번호 변경 후 기존 RefreshToken을 모두 삭제해 기존 세션을 무효화한다.
     */
    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = resetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_NOT_FOUND));

        if (token.isUsed()) throw new BusinessException(ErrorCode.RESET_TOKEN_ALREADY_USED);
        if (token.isExpired()) throw new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED);

        Member member = memberRepository.findById(UUID.fromString(token.getMemberId()))
                .orElseThrow(MemberNotFoundException::new);

        member.updatePassword(passwordEncoder.encode(newPassword));
        token.markAsUsed();
        refreshTokenRepository.deleteByMemberId(member.getId().toString()); // 기존 세션 모두 무효화
    }

    private PasswordResetToken createToken(Member member) {
        resetTokenRepository.deleteByMemberId(member.getId().toString());
        return resetTokenRepository.save(PasswordResetToken.create(member.getId().toString(), expireMinutes));
    }
}
