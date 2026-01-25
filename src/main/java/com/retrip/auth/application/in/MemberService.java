package com.retrip.auth.application.in;

import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.request.*;
import com.retrip.auth.application.in.response.*;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.entity.RefreshToken;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import com.retrip.auth.domain.vo.MemberEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService implements ManageMemberUseCase {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    public MemberCreateResponse createUser(MemberCreateRequest request) {
        String encode = passwordEncoder.encode(request.password());

        // 이메일 중복 체크 (탈퇴 회원 포함)
        List<Member> existingMembers = memberRepository.findByEmail(new MemberEmail(request.email()));

        if (!existingMembers.isEmpty()) {
            Member existing = existingMembers.get(0);
            if (existing.getIsDeleted()) {
                throw new BusinessException(ErrorCode.DELETED_MEMBER_CANNOT_REJOIN);
            }
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = memberRepository.save(request.to(encode));

        // 토큰 생성
        Authentication authentication = createAuthentication(member);
        LoginResponse.TokenResponse tokens = jwtProvider.generateTokens(authentication);

        // Refresh Token 저장
        RefreshToken refreshToken = new RefreshToken(
                tokens.refreshToken(),
                member.getId().toString(),
                "ROLE_USER"
        );
        refreshTokenRepository.save(refreshToken);

        return MemberCreateResponse.of(member, tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    public MemberUpdateResponse updateUser(UUID memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), member.getPassword().getValue())) {
            throw new BadCredentialsException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 정보 수정
        String encodedNewPassword = request.newPassword() != null
                ? passwordEncoder.encode(request.newPassword())
                : member.getPassword().getValue();

        member.update(
                request.name(),
                encodedNewPassword,
                request.gender(),
                request.age()
        );

        // Access Token 재발급
        Authentication authentication = createAuthentication(member);
        LoginResponse.TokenResponse tokens = jwtProvider.generateTokens(authentication);

        return MemberUpdateResponse.of(member, tokens.accessToken());
    }

    @Override
    public void deleteUser(UUID memberId, MemberDeleteRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), member.getPassword().getValue())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        // Refresh Token 삭제
        refreshTokenRepository.deleteByMemberId(memberId.toString());
        member.delete();
    }

    @Override
    public ChangePasswordResponse changePassword(UUID memberId, ChangePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword().getValue())) {
            throw new BadCredentialsException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호로 변경
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        member.updatePassword(encodedNewPassword);

        // 기존 Refresh Token 모두 삭제
        refreshTokenRepository.deleteByMemberId(memberId.toString());

        // 새 토큰 발급
        Authentication authentication = createAuthentication(member);
        LoginResponse.TokenResponse tokens = jwtProvider.generateTokens(authentication);

        // 새 Refresh Token 저장
        RefreshToken refreshToken = new RefreshToken(
                tokens.refreshToken(),
                memberId.toString(),
                "ROLE_USER"
        );
        refreshTokenRepository.save(refreshToken);

        return new ChangePasswordResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse getMyInfo(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        return MemberInfoResponse.of(member);
    }

    @Override
    @Transactional(readOnly = true)
    public VerifyPasswordResponse verifyPassword(UUID memberId, VerifyPasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        boolean isValid = passwordEncoder.matches(
                request.password(),
                member.getPassword().getValue()
        );

        return new VerifyPasswordResponse(isValid);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findIdByEmail(String email) {
        return memberRepository.findByEmailAndIsDeletedFalse(new MemberEmail(email))
                .stream()
                .findFirst()
                .map(Member::getId)
                .orElseThrow(MemberNotFoundException::new);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private Member findByEmail(String email) {
        return memberRepository.findByEmailAndIsDeletedFalse(new MemberEmail(email))
                .stream().findAny().orElseThrow(MemberNotFoundException::new);
    }

    private boolean isSamePassword(String password, String inputPassword) {
        if (!passwordEncoder.matches(inputPassword, password)) {
            throw new BadCredentialsException("Bad credentials");
        }
        return true;
    }

    private Authentication createAuthentication(Member member) {
        return new UsernamePasswordAuthenticationToken(
                member.getId().toString(),  // ✅ email → memberId로 변경
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}