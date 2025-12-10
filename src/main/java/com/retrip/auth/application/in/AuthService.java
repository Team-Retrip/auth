package com.retrip.auth.application.in;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.config.UsernamePasswordAuthentication;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.RefreshToken;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;

    /**
     * 토큰 재발급 비즈니스 로직
     * @Transactional을 통해 Lazy Loading 문제를 해결하고 데이터 정합성을 보장합니다.
     */
    @Transactional
    public LoginResponse.TokenResponse reissue(String requestRefreshToken) {
        // 1. 쿠키 검증 (Controller에서 null 체크를 했더라도 한 번 더 검증하거나, 여기서 처리)
        if (requestRefreshToken == null) {
            throw new IllegalArgumentException("Refresh Token이 존재하지 않습니다.");
        }

        // 2. DB에서 토큰 찾기 (RTR)
        RefreshToken storedToken = refreshTokenRepository.findByTokenValue(requestRefreshToken)
                .orElseThrow(() -> {
                    log.warn("유효하지 않은 Refresh Token 시도 감지.");
                    return new IllegalArgumentException("유효하지 않은 토큰입니다. 다시 로그인해주세요.");
                });

        // 3. 회원 정보 조회
        Member member = memberRepository.findById(storedToken.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        // 4. Authentication 객체 생성 (이제 트랜잭션 안이라 getAuthorities() 접근 가능)
        Authentication authentication = createAuthentication(member);

        // 5. 새 토큰 생성
        LoginResponse.TokenResponse newToken = jwtProvider.generateTokens(authentication);

        // 6. Refresh Token Rotation (기존 토큰 업데이트)
        long newExpiration = System.currentTimeMillis() + (jwtConfig.getRefresh().getExpireMin() * 60 * 1000L);
        storedToken.rotate(newToken.refreshToken(), newExpiration);
        // Transactional이 걸려있으므로 save 호출 없이도 더티 체킹으로 업데이트되지만, 명시적으로 호출해도 무방함

        return newToken;
    }

    // Helper Method
    private Authentication createAuthentication(Member member) {
        CustomUserDetails userDetails = new CustomUserDetails(member);

        List<GrantedAuthority> authorities = member.getAuthorities().getValues().stream()
                .map(auth -> new SimpleGrantedAuthority(auth.getGrant().getCode()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthentication(
                userDetails,
                null,
                authorities
        );
    }
}