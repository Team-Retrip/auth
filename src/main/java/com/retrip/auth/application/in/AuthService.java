package com.retrip.auth.application.in;

import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.RefreshToken;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import com.retrip.auth.domain.exception.common.InvalidValueException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public LoginResponse.TokenResponse reissue(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new BadCredentialsException("Invalid Refresh Token");
        }

        RefreshToken savedToken = refreshTokenRepository.findByTokenValue(token)
                .orElseThrow(() -> new BadCredentialsException("Refresh Token not found or member deleted"));

        Claims claims = jwtProvider.parseClaims(token);
        String memberId = claims.getSubject();

        // 회원 상태 확인
        Member member = memberRepository.findById(UUID.fromString(memberId))
                .orElseThrow(MemberNotFoundException::new);

        if (member.getIsDeleted()) {
            refreshTokenRepository.delete(savedToken);
            throw new BadCredentialsException("탈퇴한 회원입니다.");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                memberId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        LoginResponse.TokenResponse newTokens = jwtProvider.generateTokens(authentication);

        // 기존 Refresh Token 삭제
        refreshTokenRepository.delete(savedToken);

        // 새 Refresh Token 저장
        RefreshToken newRefreshToken = new RefreshToken(
                newTokens.refreshToken(),
                memberId,
                "ROLE_USER"
        );
        refreshTokenRepository.save(newRefreshToken);

        return newTokens;
    }

    @Transactional
    public void saveRefreshToken(String tokenValue, UUID memberId) {

        refreshTokenRepository.deleteByMemberId(memberId.toString());

        RefreshToken refreshToken = new RefreshToken(
                tokenValue,
                memberId.toString(),
                "ROLE_USER"
        );
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.deleteById(token);
    }
}