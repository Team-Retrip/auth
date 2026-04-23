package com.retrip.auth.application.service;

import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.dto.response.FindEmailResponse;
import com.retrip.auth.application.dto.response.PasswordResetTokenResponse;
import com.retrip.auth.application.dto.response.VerificationPendingResponse;
import com.retrip.auth.application.out.repository.*;
import com.retrip.auth.application.service.recovery.EmailVerificationStrategy;
import com.retrip.auth.application.service.recovery.PortOneVerificationStrategy;
import com.retrip.auth.domain.entity.*;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindAccountService {

    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int SESSION_EXPIRE_MINUTES = 10;

    private final PortOneVerificationStrategy portOneStrategy;
    private final EmailVerificationStrategy emailStrategy;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final MemberRepository memberRepository;
    private final MemberSocialProviderRepository socialProviderRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PortOneVerificationSessionRepository sessionRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.password-reset.expire-minutes:30}")
    private int expireMinutes;

    // ─────────────────────────────────────────────────────────────────
    // 아이디 찾기 (본인인증)
    // ─────────────────────────────────────────────────────────────────

    /**
     * CI 매칭 → 즉시 이메일 반환.
     * CI 미매칭 → 세션 생성 후 VerificationPendingResponse 반환 (HTTP 202).
     */
    public Object findEmailByVerification(String impUid) {
        CertificationInfo cert = portOneStrategy.getCertificationInfo(impUid);

        // CI 매칭 시: 즉시 반환
        if (cert.getUniqueKey() != null) {
            Member matched = portOneStrategy.findByCi(cert.getUniqueKey()).orElse(null);
            if (matched != null) {
                return buildFindEmailResponse(matched, false);
            }
        }

        // CI 미매칭: 이름+생년월일 후보 탐색
        List<Member> candidates = portOneStrategy.findCandidates(cert.getName(), cert.getBirthday());
        if (candidates.isEmpty()) throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_BY_VERIFICATION);
        if (candidates.size() > 1) log.warn("동명이인 감지 - name: {}, birthDate: {}", cert.getName(), cert.getBirthday());

        Member candidate = candidates.get(0);
        PortOneVerificationSession session = createSession(cert, candidate.getId().toString(), "FIND_EMAIL");
        return new VerificationPendingResponse(session.getId());
    }

    /** 아이디 찾기 세션: OTP를 후보 이메일로 발송. */
    public void sendFindEmailCode(String sessionId) {
        PortOneVerificationSession session = getValidSession(sessionId, "FIND_EMAIL");
        Member member = memberRepository.findById(UUID.fromString(session.getMemberId()))
                .orElseThrow(MemberNotFoundException::new);

        String code = generateCode();
        session.setEmailCode(code, OTP_EXPIRE_MINUTES);
        sessionRepository.save(session);

        try {
            emailService.sendVerificationCode(member.getEmailValue(), code);
        } catch (MailException e) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    /** 아이디 찾기 세션: OTP 확인 → CI 연결 → 이메일 반환. */
    @Transactional
    public FindEmailResponse confirmFindEmail(String sessionId, String code) {
        PortOneVerificationSession session = getValidSession(sessionId, "FIND_EMAIL");
        validateSessionCode(session, code);

        Member member = memberRepository.findById(UUID.fromString(session.getMemberId()))
                .orElseThrow(MemberNotFoundException::new);

        linkCiFromSession(member, session);
        session.markUsed();
        sessionRepository.save(session);

        return buildFindEmailResponse(member, true);
    }

    // ─────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 (본인인증)
    // ─────────────────────────────────────────────────────────────────

    /**
     * CI 매칭 → 즉시 재설정 토큰 반환.
     * CI 미매칭 → 세션 생성 후 VerificationPendingResponse 반환 (HTTP 202).
     */
    public Object issueResetTokenByVerification(String impUid) {
        CertificationInfo cert = portOneStrategy.getCertificationInfo(impUid);

        // CI 매칭 시: 즉시 토큰 발급
        if (cert.getUniqueKey() != null) {
            Member matched = portOneStrategy.findByCi(cert.getUniqueKey()).orElse(null);
            if (matched != null) {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                return tx.execute(status -> {
                    if (!matched.hasPassword()) throw new BusinessException(ErrorCode.SOCIAL_MEMBER_NO_PASSWORD_RESET);
                    return new PasswordResetTokenResponse(createToken(matched).getToken());
                });
            }
        }

        // CI 미매칭: 이름+생년월일 후보 탐색
        List<Member> candidates = portOneStrategy.findCandidates(cert.getName(), cert.getBirthday());
        if (candidates.isEmpty()) throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_BY_VERIFICATION);
        if (candidates.size() > 1) log.warn("동명이인 감지 - name: {}, birthDate: {}", cert.getName(), cert.getBirthday());

        Member candidate = candidates.get(0);
        if (!candidate.hasPassword()) throw new BusinessException(ErrorCode.SOCIAL_MEMBER_NO_PASSWORD_RESET);

        PortOneVerificationSession session = createSession(cert, candidate.getId().toString(), "PASSWORD_RESET");
        return new VerificationPendingResponse(session.getId());
    }

    /** 비밀번호 재설정 세션: 사용자가 입력한 이메일로 OTP 발송 (이메일 소유권 확인). */
    public void sendPasswordResetCode(String sessionId, String email) {
        PortOneVerificationSession session = getValidSession(sessionId, "PASSWORD_RESET");
        Member member = memberRepository.findById(UUID.fromString(session.getMemberId()))
                .orElseThrow(MemberNotFoundException::new);

        if (!member.getEmailValue().equalsIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_MATCH);
        }

        String code = generateCode();
        session.setEmailCode(code, OTP_EXPIRE_MINUTES);
        sessionRepository.save(session);

        try {
            emailService.sendVerificationCode(member.getEmailValue(), code);
        } catch (MailException e) {
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    /** 비밀번호 재설정 세션: OTP 확인 → CI 연결 → 재설정 토큰 발급. */
    @Transactional
    public PasswordResetTokenResponse confirmPasswordReset(String sessionId, String code) {
        PortOneVerificationSession session = getValidSession(sessionId, "PASSWORD_RESET");
        validateSessionCode(session, code);

        Member member = memberRepository.findById(UUID.fromString(session.getMemberId()))
                .orElseThrow(MemberNotFoundException::new);

        linkCiFromSession(member, session);
        session.markUsed();
        sessionRepository.save(session);

        return new PasswordResetTokenResponse(createToken(member).getToken());
    }

    // ─────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 (이메일 경로) — 기존 로직 유지
    // ─────────────────────────────────────────────────────────────────

    public void sendPasswordResetEmail(String email) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String[] tokenData = tx.execute(status -> {
            Member member = emailStrategy.findMember(email);
            if (!member.hasPassword()) throw new BusinessException(ErrorCode.SOCIAL_MEMBER_NO_PASSWORD_RESET);
            String token = createToken(member).getToken();
            return new String[]{member.getEmailValue(), token};
        });
        emailService.sendPasswordResetEmail(tokenData[0], tokenData[1]);
    }

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
        refreshTokenRepository.deleteByMemberId(member.getId().toString());
    }

    // ─────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────────

    private PortOneVerificationSession createSession(CertificationInfo cert, String memberId, String purpose) {
        String gender = "MALE".equals(cert.getGender()) ? "M" : "F";
        PortOneVerificationSession session = PortOneVerificationSession.create(
                memberId, cert.getUniqueKey(), cert.getUniqueInSite(),
                cert.getName(), gender, cert.getBirthday(),
                purpose, SESSION_EXPIRE_MINUTES);
        return sessionRepository.save(session);
    }

    private PortOneVerificationSession getValidSession(String sessionId, String purpose) {
        PortOneVerificationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_SESSION_NOT_FOUND));
        if (session.isUsed() || !session.getPurpose().equals(purpose))
            throw new BusinessException(ErrorCode.VERIFICATION_SESSION_NOT_FOUND);
        if (session.isExpired())
            throw new BusinessException(ErrorCode.VERIFICATION_SESSION_EXPIRED);
        return session;
    }

    private void validateSessionCode(PortOneVerificationSession session, String code) {
        if (session.getEmailCode() == null || session.isEmailCodeExpired())
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        if (!session.getEmailCode().equals(code))
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
    }

    private void linkCiFromSession(Member member, PortOneVerificationSession session) {
        if (session.getCi() != null) {
            member.updateIdentityVerification(
                    session.getName(), session.getGender(), session.getBirthDate(),
                    session.getCi(), session.getDi());
        }
    }

    private FindEmailResponse buildFindEmailResponse(Member member, boolean isNowVerified) {
        List<String> socialProviders = socialProviderRepository.findByMemberId(member.getId())
                .stream().map(MemberSocialProvider::getProvider).toList();
        List<String> providers = new ArrayList<>(socialProviders);
        if (member.hasPassword()) providers.add("local");
        if (providers.isEmpty()) providers = List.of(member.getProvider());
        return FindEmailResponse.of(member.getEmailValue(), providers, isNowVerified);
    }

    private PasswordResetToken createToken(Member member) {
        resetTokenRepository.deleteByMemberId(member.getId().toString());
        return resetTokenRepository.save(PasswordResetToken.create(member.getId().toString(), expireMinutes));
    }

    private String generateCode() {
        return String.valueOf(new SecureRandom().nextInt(900_000) + 100_000);
    }
}
