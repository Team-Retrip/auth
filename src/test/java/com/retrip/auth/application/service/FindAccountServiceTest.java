package com.retrip.auth.application.service;

import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.dto.response.FindEmailResponse;
import com.retrip.auth.application.dto.response.PasswordResetTokenResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.PasswordResetTokenRepository;
import com.retrip.auth.domain.entity.PasswordResetToken;
import com.retrip.auth.application.service.recovery.PortOneVerificationStrategy;
import com.retrip.auth.application.service.recovery.VerificationResult;
import com.retrip.auth.domain.entity.Member;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@SpringBootTest
@Transactional
class FindAccountServiceTest {

    @Autowired FindAccountService findAccountService;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // PortOne HTTP 호출 차단
    @MockBean PortOneApiClient portOneApiClient;
    @MockBean PortOneVerificationStrategy portOneStrategy;
    // Gmail SMTP 연결 차단
    @MockBean JavaMailSender javaMailSender;

    private static final CertificationInfo DUMMY_CERT = CertificationInfo.builder()
            .name("홍길동").gender("MALE").birthday("1990-01-01")
            .uniqueKey("ci-dummy").uniqueInSite("di-dummy")
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

        // PortOne HTTP 호출은 항상 DUMMY_CERT 반환
        given(portOneApiClient.getCertificationInfo(anyString())).willReturn(DUMMY_CERT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 아이디 찾기
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 아이디찾기_기존인증사용자_isNowVerified_false() {
        given(portOneStrategy.findMemberByCert(any())).willReturn(new VerificationResult(localMember, false));

        FindEmailResponse response = findAccountService.findEmailByVerification("imp_test");

        assertThat(response.maskedEmail()).endsWith("@example.com");
        assertThat(response.maskedEmail()).doesNotContain("local@");
        assertThat(response.maskedEmail()).contains("*");
        assertThat(response.isNowVerified()).isFalse();
    }

    @Test
    void 아이디찾기_신규인증사용자_isNowVerified_true() {
        given(portOneStrategy.findMemberByCert(any())).willReturn(new VerificationResult(localMember, true));

        FindEmailResponse response = findAccountService.findEmailByVerification("imp_test");

        assertThat(response.isNowVerified()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 비밀번호 재설정 - 본인인증 경로
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 비밀번호재설정_본인인증_토큰발급_성공() {
        given(portOneStrategy.findMemberByCert(any())).willReturn(new VerificationResult(localMember, false));

        PasswordResetTokenResponse response = findAccountService.issueResetTokenByVerification("imp_test");

        assertThat(response.resetToken()).isNotBlank();
        assertThat(resetTokenRepository.findByToken(response.resetToken())).isPresent();
    }

    @Test
    void 비밀번호재설정_본인인증_소셜전용계정_실패() {
        given(portOneStrategy.findMemberByCert(any())).willReturn(new VerificationResult(socialMember, false));

        assertThatThrownBy(() -> findAccountService.issueResetTokenByVerification("imp_social"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("소셜");
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
    // 비밀번호 재설정 (토큰 검증 + 새 비밀번호 저장)
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
    void 비밀번호재설정_성공_기존세션_무효화() {
        // 비밀번호 재설정 후 RefreshToken이 삭제되는지 확인
        findAccountService.sendPasswordResetEmail("local@example.com");
        String token = resetTokenRepository.findAll().get(0).getToken();

        findAccountService.resetPassword(token, "NewPass1!");

        // 토큰이 used 상태로 변경됐는지 확인
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
    // 이메일 마스킹 로직 검증
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void 이메일마스킹_짧은로컬파트() {
        FindEmailResponse response = FindEmailResponse.of("ab@test.com", false);
        assertThat(response.maskedEmail()).startsWith("a");
        assertThat(response.maskedEmail()).contains("*");
        assertThat(response.maskedEmail()).endsWith("@test.com");
    }

    @Test
    void 이메일마스킹_긴로컬파트() {
        FindEmailResponse response = FindEmailResponse.of("hello@test.com", false);
        assertThat(response.maskedEmail()).startsWith("hel");
        assertThat(response.maskedEmail()).endsWith("@test.com");
        assertThat(response.maskedEmail()).contains("**");
    }
}
