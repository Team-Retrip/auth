package com.retrip.auth.application.in;

import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.RefreshToken;
import com.retrip.auth.domain.exception.common.ErrorCode;
import com.retrip.auth.domain.exception.common.InvalidValueException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponse.TokenResponse reissue(String token) {
        if (!jwtProvider.validateToken(token)) {
            throw new InvalidValueException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않거나 만료된 토큰입니다.");
        }

        RefreshToken savedToken = refreshTokenRepository.findById(token)
                .orElseThrow(() -> new InvalidValueException(ErrorCode.ENTITY_NOT_FOUND, "로그아웃 되었거나 존재하지 않는 토큰입니다."));

        Claims claims = jwtProvider.parseClaims(token);
        String memberId = claims.getSubject();
        String authoritiesStr = (String) claims.get("authorities");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                memberId,
                null,
                Arrays.stream(authoritiesStr.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())
        );

        LoginResponse.TokenResponse newTokens = jwtProvider.generateTokens(auth);

        refreshTokenRepository.delete(savedToken);
        refreshTokenRepository.save(new RefreshToken(newTokens.refreshToken(), memberId, authoritiesStr));

        return newTokens;
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.deleteById(token);
    }
}