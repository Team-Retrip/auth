package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.in.request.*;
import com.retrip.auth.application.in.request.SetInitialPasswordRequest;
import com.retrip.auth.application.in.response.*;
import com.retrip.auth.application.in.response.MemberSearchResponse;
import com.retrip.auth.application.in.usercase.ManageMemberUseCase;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 관련 API")
public class MemberController {

    private final ManageMemberUseCase manageMemberUseCase;

    @PostMapping
    @Schema(description = "회원 가입")
    public ResponseEntity<ApiResponse<MemberCreateResponse>> createUser(
            @Valid @RequestBody MemberCreateRequest request) {
        MemberCreateResponse response = manageMemberUseCase.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PutMapping
    @Schema(description = "회원 정보 수정")
    public ApiResponse<MemberUpdateResponse> updateUser(
            Authentication authentication,
            @RequestBody MemberUpdateRequest request) {
        UUID memberId = extractMemberId(authentication);
        return ApiResponse.ok(manageMemberUseCase.updateUser(memberId, request));
    }

    @DeleteMapping
    @Schema(description = "회원 정보 삭제")
    public ResponseEntity<ApiResponse<?>> deleteUser(
            Authentication authentication,
            @RequestBody MemberDeleteRequest request) {
        UUID memberId = extractMemberId(authentication);
        manageMemberUseCase.deleteUser(memberId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.noContent());
    }

    @PostMapping("/password")
    @Schema(description = "소셜 유저 최초 비밀번호 설정")
    public ResponseEntity<ApiResponse<?>> setInitialPassword(
            Authentication authentication,
            @RequestBody SetInitialPasswordRequest request) {
        UUID memberId = extractMemberId(authentication);
        manageMemberUseCase.setInitialPassword(memberId, request.password());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/password")
    @Schema(description = "비밀번호 변경")
    public ApiResponse<ChangePasswordResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID memberId = extractMemberId(authentication);
        return ApiResponse.ok(manageMemberUseCase.changePassword(memberId, request));
    }

    @GetMapping("/me")
    @Schema(description = "내 정보 조회")
    public ApiResponse<MemberInfoResponse> getMyInfo(Authentication authentication) {
        UUID memberId = extractMemberId(authentication);
        return ApiResponse.ok(manageMemberUseCase.getMyInfo(memberId));
    }

    @PostMapping("/verify-password")
    @Schema(description = "비밀번호 확인")
    public ApiResponse<VerifyPasswordResponse> verifyPassword(
            Authentication authentication,
            @RequestBody VerifyPasswordRequest request) {
        UUID memberId = extractMemberId(authentication);
        return ApiResponse.ok(manageMemberUseCase.verifyPassword(memberId, request));
    }

    @GetMapping("/search")
    @Schema(description = "이름으로 회원 검색")
    public ApiResponse<List<MemberSearchResponse>> searchMembers(@RequestParam String name) {
        return ApiResponse.ok(manageMemberUseCase.searchMembers(name));
    }

    @GetMapping("/members")
    @Schema(description = "ID 목록으로 회원 정보 일괄 조회")
    public ApiResponse<List<MemberSearchResponse>> getMembersByIds(@RequestParam List<UUID> ids) {
        return ApiResponse.ok(manageMemberUseCase.getMembersByIds(ids));
    }

    // JWT에서 memberId 추출
    private UUID extractMemberId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            log.error("인증 정보가 없습니다.");
            throw new MemberNotFoundException();
        }

        Object principal = authentication.getPrincipal();
        String memberIdStr;

        if (principal instanceof String) {
            memberIdStr = (String) principal;
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // UserDetails 구현체라면 username(여기서는 memberId)을 가져옴
            memberIdStr = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            log.error("알 수 없는 Principal 타입: {}", principal.getClass().getName());
            throw new MemberNotFoundException();
        }

        try {
            return UUID.fromString(memberIdStr);
        } catch (IllegalArgumentException e) {
            log.error("유효하지 않은 UUID 형식: {}", memberIdStr);
            throw new MemberNotFoundException();
        }
    }
}