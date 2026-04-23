package com.retrip.auth.application.service;

import com.retrip.auth.application.out.repository.EmailVerificationCodeRepository;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.EmailVerificationCode;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import com.retrip.auth.domain.vo.MemberEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 10;
    private static final int VERIFIED_VALID_MINUTES = 30;

    private final EmailVerificationCodeRepository codeRepository;
    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 회원가입용 이메일 인증코드를 발송한다.
     * 이미 가입된 이메일이면 거부, 탈퇴한 이메일이면 별도 에러.
     */
    public void sendSignupCode(String email) {
        memberRepository.findByEmail(new MemberEmail(email)).forEach(m -> {
            if (m.getIsDeleted()) throw new BusinessException(ErrorCode.DELETED_MEMBER_CANNOT_REJOIN);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        });

        String code = generateCode();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            codeRepository.deleteByEmail(email);
            return codeRepository.save(EmailVerificationCode.create(email, code, CODE_EXPIRE_MINUTES));
        });

        try {
            emailService.sendVerificationCode(email, code);
        } catch (MailException e) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    /**
     * 인증코드를 검증하고 verified 상태로 마킹한다.
     */
    public void verifySignupCode(String email, String code) {
        EmailVerificationCode record = codeRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_CODE_NOT_FOUND));

        if (record.isExpired()) throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        if (!record.getCode().equals(code)) throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);

        record.markVerified();
        codeRepository.save(record);
    }

    /**
     * 회원가입 직전에 해당 이메일이 최근 30분 내 인증 완료됐는지 확인한다.
     */
    public void assertVerified(String email) {
        boolean verified = codeRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .map(r -> r.isVerifiedWithin(VERIFIED_VALID_MINUTES))
                .orElse(false);
        if (!verified) throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(900_000) + 100_000;
        return String.valueOf(number);
    }
}
