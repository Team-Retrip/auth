package com.retrip.auth.application.service;

import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.dto.response.FindEmailResponse;
import com.retrip.auth.application.dto.response.PasswordResetTokenResponse;
import com.retrip.auth.application.dto.response.VerificationPendingResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.PasswordResetTokenRepository;
import com.retrip.auth.application.out.repository.PortOneVerificationSessionRepository;
import com.retrip.auth.application.service.recovery.PortOneVerificationStrategy;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.PasswordResetToken;
import com.retrip.auth.domain.entity.PortOneVerificationSession;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.infra.adapter.out.external.PortOneApiClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@Transactional
class FindAccountServiceTest {

    @Autowired FindAccountService findAccountService;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired PortOneVerificationSessionRepository sessionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean PortOneApiClient portOneApiClient;
    @MockBean PortOneVerificationStrategy portOneStrategy;
    @MockBean JavaMailSender javaMailSender;

    private static final CertificationInfo CERT_WITH_CI = CertificationInfo.builder()
            .name("홍길동").gender("MALE").birthday("1990-01-01")
            .uniqueKey("ci-test").uniqueInSite("di-test")
            .build();

    private static final CertificationInfo CERT_WITHOUT_CI = CertificationInfo.builder()
            .name("홍길동").gender("MALE").birthday("1990-01-01")
            .uniqueKey(null).uniqueInSite(null)
            .build();

    private Member localMember;
    private Member socialMember;

    @BeforeEach
    void setUp() {
        localMember = memberRepository.save(Member.create(
                "홍길동", "local@example.com", passwordEncoder.encode("Test1234!"),
                List.of("user"), "M", "1990-01-01", true, false, null
        ));
        socialMember = memberRepository.save(Member.createSocialMember(
                "김소셜", "social@example.com", "google"
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 아이디 찾기 - CI 매칭 경로
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 아이디찾기_CI매칭_즉시이메일반환() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.of(localMember));

        Object result = findAccountService.findEmailByVerification("imp_test");

        assertThat(result).isInstanceOf(FindEmailResponse.class);
        FindEmailResponse response = (FindEmailResponse) result;
        assertThat(response.maskedEmail()).endsWith("@example.com");
        assertThat(response.maskedEmail()).contains("*");
        assertThat(response.isNowVerified()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 아이디 찾기 - CI 미매칭 → OTP 플로우
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 아이디찾기_CI미매칭_세션반환() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        Object result = findAccountService.findEmailByVerification("imp_test");

        assertThat(result).isInstanceOf(VerificationPendingResponse.class);
        VerificationPendingResponse pending = (VerificationPendingResponse) result;
        assertThat(pending.sessionId()).isNotBlank();
        assertThat(sessionRepository.findById(pending.sessionId())).isPresent();
    }

    @Test
    void 아이디찾기_CI없음_세션반환() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITHOUT_CI);
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        Object result = findAccountService.findEmailByVerification("imp_test");

        assertThat(result).isInstanceOf(VerificationPendingResponse.class);
    }

    @Test
    void 아이디찾기_후보없음_예외() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITHOUT_CI);
        given(portOneStrategy.findCandidates(anyString(), anyString())).willReturn(List.of());

        assertThatThrownBy(() -> findAccountService.findEmailByVerification("imp_test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("계정을 찾을 수 없습니다");
    }

    @Test
    void 아이디찾기_OTP발송_성공() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        VerificationPendingResponse pending = (VerificationPendingResponse) findAccountService.findEmailByVerification("imp_test");

        findAccountService.sendFindEmailCode(pending.sessionId());

        then(javaMailSender).should().send(any(org.springframework.mail.SimpleMailMessage.class));
        PortOneVerificationSession session = sessionRepository.findById(pending.sessionId()).orElseThrow();
        assertThat(session.getEmailCode()).isNotBlank();
    }

    @Test
    void 아이디찾기_OTP확인_성공_이메일반환() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        VerificationPendingResponse pending = (VerificationPendingResponse) findAccountService.findEmailByVerification("imp_test");
        findAccountService.sendFindEmailCode(pending.sessionId());

        String code = sessionRepository.findById(pending.sessionId()).orElseThrow().getEmailCode();
        FindEmailResponse result = findAccountService.confirmFindEmail(pending.sessionId(), code);

        assertThat(result.maskedEmail()).endsWith("@example.com");
        assertThat(result.isNowVerified()).isTrue();
        assertThat(sessionRepository.findById(pending.sessionId()).orElseThrow().isUsed()).isTrue();
    }

    @Test
    void 아이디찾기_OTP틀림_예외() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        VerificationPendingResponse pending = (VerificationPendingResponse) findAccountService.findEmailByVerification("imp_test");
        findAccountService.sendFindEmailCode(pending.sessionId());

        assertThatThrownBy(() -> findAccountService.confirmFindEmail(pending.sessionId(), "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("올바르지 않습니다");
    }

    @Test
    void 아이디찾기_OTP발송_세션없음_예외() {
        assertThatThrownBy(() -> findAccountService.sendFindEmailCode("non-existent-session"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("인증 세션을 찾을 수 없");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 - 본인인증 경로
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 비밀번호재설정_CI매칭_즉시토큰발급() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.of(localMember));

        Object result = findAccountService.issueResetTokenByVerification("imp_test");

        assertThat(result).isInstanceOf(PasswordResetTokenResponse.class);
        PasswordResetTokenResponse response = (PasswordResetTokenResponse) result;
        assertThat(response.resetToken()).isNotBlank();
        assertThat(resetTokenRepository.findByToken(response.resetToken())).isPresent();
    }

    @Test
    void 비밀번호재설정_CI미매칭_세션반환() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        Object result = findAccountService.issueResetTokenByVerification("imp_test");

        assertThat(result).isInstanceOf(VerificationPendingResponse.class);
    }

    @Test
    void 비밀번호재설정_CI매칭_소셜전용계정_예외() {
        given(portOneStrategy.getCertificationInfo("imp_social")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.of(socialMember));

        assertThatThrownBy(() -> findAccountService.issueResetTokenByVerification("imp_social"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("소셜");
    }

    @Test
    void 비밀번호재설정_CI미매칭_소셜전용계정_예외() {
        given(portOneStrategy.getCertificationInfo("imp_social")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(socialMember));

        assertThatThrownBy(() -> findAccountService.issueResetTokenByVerification("imp_social"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("소셜");
    }

    @Test
    void 비밀번호재설정_OTP발송_이메일검증_성공() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        VerificationPendingResponse pending = (VerificationPendingResponse) findAccountService.issueResetTokenByVerification("imp_test");

        findAccountService.sendPasswordResetCode(pending.sessionId(), "local@example.com");

        then(javaMailSender).should().send(any(org.springframework.mail.SimpleMailMessage.class));
        PortOneVerificationSession session = sessionRepository.findById(pending.sessionId()).orElseThrow();
        assertThat(session.getEmailCode()).isNotBlank();
    }

    @Test
    void 비밀번호재설정_OTP발송_이메일불일치_예외() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        VerificationPendingResponse pending = (VerificationPendingResponse) findAccountService.issueResetTokenByVerification("imp_test");

        assertThatThrownBy(() -> findAccountService.sendPasswordResetCode(pending.sessionId(), "other@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이메일이 등록된 정보와 일치하지 않습니다");
    }

    @Test
    void 비밀번호재설정_OTP확인_성공_토큰발급() {
        given(portOneStrategy.getCertificationInfo("imp_test")).willReturn(CERT_WITH_CI);
        given(portOneStrategy.findByCi("ci-test")).willReturn(Optional.empty());
        given(portOneStrategy.findCandidates("홍길동", "1990-01-01")).willReturn(List.of(localMember));

        VerificationPendingResponse pending = (VerificationPendingResponse) findAccountService.issueResetTokenByVerification("imp_test");
        findAccountService.sendPasswordResetCode(pending.sessionId(), "local@example.com");

        String code = sessionRepository.findById(pending.sessionId()).orElseThrow().getEmailCode();
        PasswordResetTokenResponse result = findAccountService.confirmPasswordReset(pending.sessionId(), code);

        assertThat(result.resetToken()).isNotBlank();
        assertThat(resetTokenRepository.findByToken(result.resetToken())).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 - 이메일 경로
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 비밀번호재설정_이메일발송_성공_토큰생성_확인() {
        findAccountService.sendPasswordResetEmail("local@example.com");

        then(javaMailSender).should().send(any(org.springframework.mail.SimpleMailMessage.class));
        assertThat(resetTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void 비밀번호재설정_이메일_존재하지않는계정_실패() {
        assertThatThrownBy(() -> findAccountService.sendPasswordResetEmail("none@example.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 비밀번호재설정_이메일_소셜전용계정_실패() {
        assertThatThrownBy(() -> findAccountService.sendPasswordResetEmail("social@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("소셜");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 - 토큰 검증 + 새 비밀번호 저장
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 비밀번호재설정_성공() {
        findAccountService.sendPasswordResetEmail("local@example.com");
        String token = resetTokenRepository.findAll().get(0).getToken();

        findAccountService.resetPassword(token, "NewPass1!");

        Member updated = memberRepository.findById(localMember.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPass1!", updated.getPasswordValue())).isTrue();
    }

    @Test
    void 비밀번호재설정_성공_토큰_used_마킹() {
        findAccountService.sendPasswordResetEmail("local@example.com");
        String token = resetTokenRepository.findAll().get(0).getToken();

        findAccountService.resetPassword(token, "NewPass1!");

        PasswordResetToken used = resetTokenRepository.findByToken(token).orElseThrow();
        assertThat(used.isUsed()).isTrue();
    }

    @Test
    void 비밀번호재설정_재발급시_기존토큰_무효화() {
        findAccountService.sendPasswordResetEmail("local@example.com");
        String firstToken = resetTokenRepository.findAll().get(0).getToken();

        findAccountService.sendPasswordResetEmail("local@example.com");

        assertThat(resetTokenRepository.findByToken(firstToken)).isEmpty();
        assertThat(resetTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void 비밀번호재설정_유효하지않은토큰_실패() {
        assertThatThrownBy(() -> findAccountService.resetPassword("invalid-token", "NewPass1!"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    void 비밀번호재설정_이미사용된토큰_실패() {
        findAccountService.sendPasswordResetEmail("local@example.com");
        String token = resetTokenRepository.findAll().get(0).getToken();
        findAccountService.resetPassword(token, "NewPass1!");

        assertThatThrownBy(() -> findAccountService.resetPassword(token, "NewPass2!"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 사용된");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 이메일 마스킹 로직
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 이메일마스킹_짧은로컬파트() {
        FindEmailResponse response = FindEmailResponse.of("ab@test.com", List.of("local"), false);
        assertThat(response.maskedEmail()).startsWith("a");
        assertThat(response.maskedEmail()).contains("*");
        assertThat(response.maskedEmail()).endsWith("@test.com");
    }

    @Test
    void 이메일마스킹_긴로컬파트() {
        FindEmailResponse response = FindEmailResponse.of("hello@test.com", List.of("local"), false);
        assertThat(response.maskedEmail()).startsWith("hel");
        assertThat(response.maskedEmail()).endsWith("@test.com");
        assertThat(response.maskedEmail()).contains("**");
    }
}
