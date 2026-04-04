package com.retrip.auth.application.in;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.request.*;
import com.retrip.auth.application.in.response.*;
import com.retrip.auth.application.in.response.MemberSearchResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        if (!request.termsAgreed()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }

        if (request.birthDate() != null) {
            LocalDate birth = LocalDate.parse(request.birthDate());
            if (birth.isAfter(LocalDate.now())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (birth.isBefore(LocalDate.now().minusYears(100))) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        String encode = passwordEncoder.encode(request.password());

        List<Member> existingMembers = memberRepository.findByEmail(new MemberEmail(request.email()));
        if (!existingMembers.isEmpty()) {
            Member existing = existingMembers.get(0);
            if (existing.getIsDeleted()) throw new BusinessException(ErrorCode.DELETED_MEMBER_CANNOT_REJOIN);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = memberRepository.save(request.to(encode));

        Authentication authentication = createAuthentication(member);
        LoginResponse.TokenResponse tokens = jwtProvider.generateTokens(authentication);

        RefreshToken refreshToken = new RefreshToken(tokens.refreshToken(), member.getId().toString(), "ROLE_USER");
        refreshTokenRepository.save(refreshToken);

        return MemberCreateResponse.of(member, tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    public MemberUpdateResponse updateUser(UUID memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        // 본인인증 완료 후 이름/생년월일 변경 불가
        if (member.isVerified()) {
            boolean tryName = request.name() != null && !request.name().equals(member.getNameValue());
            boolean tryBirth = request.birthDate() != null && !request.birthDate().equals(member.getBirthDate());
            if (tryName || tryBirth) throw new BusinessException(ErrorCode.VERIFIED_MEMBER_CANNOT_CHANGE);
        }

        // 비밀번호 있는 계정만 현재 비밀번호 검증
        if (member.hasPassword()) {
            if (!passwordEncoder.matches(request.password(), member.getPasswordValue()))
                throw new BadCredentialsException("현재 비밀번호가 일치하지 않습니다.");
        }

        String encodedNewPassword = request.newPassword() != null
                ? passwordEncoder.encode(request.newPassword())
                : member.getPasswordValue();

        member.update(request.name(), encodedNewPassword, request.gender(), request.birthDate());

        Authentication authentication = createAuthentication(member);
        LoginResponse.TokenResponse tokens = jwtProvider.generateTokens(authentication);

        return MemberUpdateResponse.of(member, tokens.accessToken());
    }

    @Override
    public void deleteUser(UUID memberId, MemberDeleteRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        // 비밀번호 있는 계정만 비밀번호 검증
        if (member.hasPassword()) {
            if (!passwordEncoder.matches(request.password(), member.getPasswordValue()))
                throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        refreshTokenRepository.deleteByMemberId(memberId.toString());
        member.delete();
    }

    @Override
    public ChangePasswordResponse changePassword(UUID memberId, ChangePasswordRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        // 소셜 전용 계정(비밀번호 없음) 차단
        if (!member.hasPassword()) throw new BusinessException(ErrorCode.SOCIAL_MEMBER_CANNOT_CHANGE_PASSWORD);

        if (!passwordEncoder.matches(request.currentPassword(), member.getPasswordValue()))
            throw new BadCredentialsException("현재 비밀번호가 일치하지 않습니다.");

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        member.updatePassword(encodedNewPassword);

        refreshTokenRepository.deleteByMemberId(memberId.toString());

        Authentication authentication = createAuthentication(member);
        LoginResponse.TokenResponse tokens = jwtProvider.generateTokens(authentication);

        RefreshToken refreshToken = new RefreshToken(tokens.refreshToken(), memberId.toString(), "ROLE_USER");
        refreshTokenRepository.save(refreshToken);

        return new ChangePasswordResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @Override
    public void setInitialPassword(UUID memberId, String rawPassword) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        member.setPassword(passwordEncoder.encode(rawPassword));
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse getMyInfo(UUID memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        return MemberInfoResponse.of(member);
    }

    @Override
    @Transactional(readOnly = true)
    public VerifyPasswordResponse verifyPassword(UUID memberId, VerifyPasswordRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        boolean isValid = member.hasPassword() &&
                passwordEncoder.matches(request.password(), member.getPasswordValue());
        return new VerifyPasswordResponse(isValid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberSearchResponse> searchMembers(String name) {
        return memberRepository.searchByNameContainingIgnoreCase(name)
                .stream()
                .map(MemberSearchResponse::of)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberSearchResponse> getMembersByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return memberRepository.findAllByIdInAndIsDeletedFalse(ids)
                .stream()
                .map(MemberSearchResponse::of)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findIdByEmail(String email) {
        return memberRepository.findByEmailAndIsDeletedFalse(new MemberEmail(email))
                .stream().findFirst()
                .map(Member::getId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private Authentication createAuthentication(Member member) {
        // Hibernate 세션 내에서 authorities 강제 초기화 (LazyInitializationException 방지)
        member.getAuthorities().getValues().size();
        CustomUserDetails userDetails = new CustomUserDetails(member);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
