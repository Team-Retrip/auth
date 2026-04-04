package com.retrip.auth.application.in;

import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.RefreshToken;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("토큰 재발급(reissue) 성공 테스트")
    void reissue_Success() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String memberId = "00000000-0000-0000-0000-000000000001";
        String authorities = "ROLE_USER";

        given(jwtProvider.validateToken(oldRefreshToken)).willReturn(true);

        RefreshToken savedToken = new RefreshToken(oldRefreshToken, memberId, authorities);
        given(refreshTokenRepository.findByTokenValue(oldRefreshToken)).willReturn(Optional.of(savedToken));

        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(memberId);

        given(jwtProvider.parseClaims(oldRefreshToken)).willReturn(claims);

        Member mockMember = mock(Member.class);
        given(mockMember.getIsDeleted()).willReturn(false);
        given(memberRepository.findById(any())).willReturn(Optional.of(mockMember));

        LoginResponse.TokenResponse newTokens = new LoginResponse.TokenResponse("new-access", "new-refresh");
        given(jwtProvider.generateTokens(any(Authentication.class))).willReturn(newTokens);

        // when
        LoginResponse.TokenResponse result = authService.reissue(oldRefreshToken);

        // then
        assertNotNull(result);
        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());

        verify(refreshTokenRepository).delete(savedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
}