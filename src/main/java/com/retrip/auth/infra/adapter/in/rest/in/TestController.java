package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Authorities;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.vo.MemberEmail;
import com.retrip.auth.domain.vo.MemberName;
import com.retrip.auth.domain.vo.MemberPassword;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@Profile({"local", "dev"})
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class TestController {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 테스트용 백도어 API
     * 프론트엔드 개발자가 로그인 없이 JWT 토큰을 발급받을 수 있도록 제공
     * 
     * ⚠️ 주의: local, dev 프로파일에서만 활성화됩니다.
     */
    @GetMapping("/test-token")
    @Transactional
    public ApiResponse<String> getTestToken() {
        log.warn("🚨 테스트 토큰 발급 - 개발 환경 전용 API");

        UUID testUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String testEmail = "test@test.com";
        
        // 1. 기존 테스트 사용자 조회 (이메일로)
        Member testMember = memberRepository.findByEmail_Value(testEmail)
                .filter(m -> !m.getIsDeleted())  // 탈퇴하지 않은 사용자만
                .orElseGet(() -> {
                    log.info("🔨 테스트 사용자 생성 중...");
                    
                    // 2. Member.create() 사용
                    Member newMember = Member.create(
                            "테스트유저",
                            testEmail,
                            passwordEncoder.encode("test1234"),
                            List.of("user"),
                            "M",
                            "1995-01-01",
                            true,  // termsAgreed
                            true   // marketingAgreed
                    );
                    
                    // 3. DB 저장
                    return memberRepository.save(newMember);
                });

        log.info("✅ 테스트 사용자 확인 - UUID: {}, Email: {}", 
                testMember.getId(), testMember.getEmailValue());

        // CustomUserDetails로 감싸기
        CustomUserDetails userDetails = new CustomUserDetails(testMember);

        // Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // JWT 토큰 생성
        LoginResponse.TokenResponse tokens = jwtProvider.generateTokens(authentication);

        log.info("✅ 테스트 토큰 발급 완료");

        return ApiResponse.ok(tokens.accessToken());
    }
}
