package com.retrip.auth.infra.adapter.in.rest.controller;

import com.retrip.auth.application.dto.request.FindEmailByVerificationRequest;
import com.retrip.auth.application.dto.request.ResetPasswordByEmailRequest;
import com.retrip.auth.application.dto.request.ResetPasswordByVerificationRequest;
import com.retrip.auth.application.dto.request.ResetPasswordRequest;
import com.retrip.auth.application.dto.response.FindEmailResponse;
import com.retrip.auth.application.dto.response.PasswordResetTokenResponse;
import com.retrip.auth.application.service.FindAccountService;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "계정 찾기", description = "아이디/비밀번호 찾기 API")
public class FindAccountController {

    private final FindAccountService findAccountService;

    @Operation(summary = "아이디 찾기 (본인인증)",
            description = "PortOne 본인인증으로 가입 이메일을 조회합니다. 미인증 사용자는 이번 인증으로 자동 연결됩니다.")
    @PostMapping("/find-email")
    public ApiResponse<FindEmailResponse> findEmail(
            @Valid @RequestBody FindEmailByVerificationRequest request) {
        return ApiResponse.ok(findAccountService.findEmailByVerification(request.impUid()));
    }

    @Operation(summary = "비밀번호 재설정 토큰 발급 (본인인증)",
            description = "PortOne 본인인증으로 신원 확인 후 비밀번호 재설정 토큰을 발급합니다. (30분 유효, 1회용)")
    @PostMapping("/password-reset/by-verification")
    public ApiResponse<PasswordResetTokenResponse> requestResetByVerification(
            @Valid @RequestBody ResetPasswordByVerificationRequest request) {
        return ApiResponse.ok(findAccountService.issueResetTokenByVerification(request.impUid()));
    }

    @Operation(summary = "비밀번호 재설정 이메일 발송",
            description = "가입 이메일로 비밀번호 재설정 링크를 전송합니다. 소셜 전용 계정은 불가합니다.")
    @PostMapping("/password-reset/by-email")
    public ResponseEntity<ApiResponse<Void>> requestResetByEmail(
            @Valid @RequestBody ResetPasswordByEmailRequest request) {
        findAccountService.sendPasswordResetEmail(request.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
    }

    @Operation(summary = "비밀번호 재설정",
            description = "재설정 토큰과 새 비밀번호로 비밀번호를 변경합니다.")
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        findAccountService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
    }
}
