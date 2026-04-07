package com.retrip.auth.infra.adapter.in.rest.controller;

import com.retrip.auth.application.dto.request.UpdateNotificationRequest;
import com.retrip.auth.application.dto.request.UpdateProfileRequest;
import com.retrip.auth.application.dto.request.VerifyIdentityRequest;
import com.retrip.auth.application.dto.response.ProfileResponse;
import com.retrip.auth.application.dto.response.VerifyIdentityResponse;
import com.retrip.auth.application.service.IdentityVerificationService;
import com.retrip.auth.application.service.ProfileService;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final IdentityVerificationService identityVerificationService;

    /**
     * 내 프로필 조회
     */
    @GetMapping("/users/profile")
    public ApiResponse<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String memberId = userDetails.getUsername();
        ProfileResponse profile = profileService.getProfile(memberId);
        return ApiResponse.ok(profile);
    }

    /**
     * 프로필 수정
     */
    @PutMapping("/users/profile")
    public ApiResponse<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String memberId = userDetails.getUsername();
        ProfileResponse profile = profileService.updateProfile(memberId, request);
        return ApiResponse.ok(profile);
    }

    /**
     * 닉네임 중복 확인 (비로그인 가능)
     */
    @GetMapping("/users/check-nickname")
    public ApiResponse<Boolean> checkNickname(
            @RequestParam @NotBlank @Size(max = 15) String nickname
    ) {
        return ApiResponse.ok(profileService.isNicknameAvailable(nickname));
    }

    /**
     * 알림 설정 변경
     */
    @PatchMapping("/users/notification-settings")
    public ApiResponse<Void> updateNotificationSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNotificationRequest request
    ) {
        String memberId = userDetails.getUsername();
        profileService.updateNotificationSettings(memberId, request);
        return ApiResponse.ok(null);
    }

    /**
     * 본인인증 검증 및 정보 저장
     */
    @PostMapping("/auth/verify-identity")
    public ApiResponse<VerifyIdentityResponse> verifyIdentity(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyIdentityRequest request
    ) {
        String memberId = userDetails.getUsername();
        VerifyIdentityResponse response = identityVerificationService.verifyAndSave(
                request.getImpUid(),
                memberId
        );
        return ApiResponse.ok(response);
    }
}
