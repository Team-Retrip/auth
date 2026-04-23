package com.retrip.auth.infra.adapter.in.rest.controller;

import com.retrip.auth.application.dto.request.ConfirmSessionRequest;
import com.retrip.auth.application.dto.request.FindEmailByVerificationRequest;
import com.retrip.auth.application.dto.request.ResetPasswordByEmailRequest;
import com.retrip.auth.application.dto.request.ResetPasswordByVerificationRequest;
import com.retrip.auth.application.dto.request.ResetPasswordRequest;
import com.retrip.auth.application.dto.request.SendSessionEmailCodeRequest;
import com.retrip.auth.application.dto.request.SessionIdRequest;
import com.retrip.auth.application.dto.response.FindEmailResponse;
import com.retrip.auth.application.dto.response.PasswordResetTokenResponse;
import com.retrip.auth.application.dto.response.VerificationPendingResponse;
import com.retrip.auth.application.service.FindAccountService;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "계정 찾기", description = "아이디/비밀번호 찾기 API")
public class FindAccountController {

    private final FindAccountService findAccountService;

    @Operation(summary = "아이디 찾기 (본인인증)",
            description = """
                    PortOne 본인인증으로 가입 이메일을 조회합니다.
                    - CI 매칭 성공(200): 이메일 즉시 반환
                    - CI 미매칭(202): sessionId 반환 → /auth/find-email/send-code → /auth/find-email/confirm 순서로 진행
                    """)
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<?>> findEmail(
            @Valid @RequestBody FindEmailByVerificationRequest request) {
        Object result = findAccountService.findEmailByVerification(request.impUid());
        if (result instanceof FindEmailResponse r)
            return ResponseEntity.ok(ApiResponse.ok(r));
        return ResponseEntity.accepted().body(ApiResponse.of((VerificationPendingResponse) result, HttpStatus.ACCEPTED));
    }

    @Operation(summary = "아이디 찾기 - OTP 발송",
            description = "find-email에서 202를 받은 경우, 등록된 이메일로 OTP를 발송합니다.")
    @PostMapping("/find-email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendFindEmailCode(
            @Valid @RequestBody SessionIdRequest request) {
        findAccountService.sendFindEmailCode(request.sessionId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
    }

    @Operation(summary = "아이디 찾기 - OTP 확인",
            description = "OTP를 확인하고 이메일을 반환합니다. 성공 시 본인인증(CI)이 계정에 자동 연결됩니다.")
    @PostMapping("/find-email/confirm")
    public ApiResponse<FindEmailResponse> confirmFindEmail(
            @Valid @RequestBody ConfirmSessionRequest request) {
        return ApiResponse.ok(findAccountService.confirmFindEmail(request.sessionId(), request.code()));
    }

    @Operation(summary = "비밀번호 재설정 토큰 발급 (본인인증)",
            description = """
                    PortOne 본인인증으로 신원 확인 후 비밀번호 재설정 토큰을 발급합니다.
                    - CI 매칭 성공(200): 재설정 토큰 즉시 반환
                    - CI 미매칭(202): sessionId 반환 → /auth/password-reset/by-verification/send-code → /confirm 순서로 진행
                    """)
    @PostMapping("/password-reset/by-verification")
    public ResponseEntity<ApiResponse<?>> requestResetByVerification(
            @Valid @RequestBody ResetPasswordByVerificationRequest request) {
        Object result = findAccountService.issueResetTokenByVerification(request.impUid());
        if (result instanceof PasswordResetTokenResponse r)
            return ResponseEntity.ok(ApiResponse.ok(r));
        return ResponseEntity.accepted().body(ApiResponse.of((VerificationPendingResponse) result, HttpStatus.ACCEPTED));
    }

    @Operation(summary = "비밀번호 재설정 - OTP 발송",
            description = "by-verification에서 202를 받은 경우, 가입 이메일을 입력하면 OTP를 발송합니다.")
    @PostMapping("/password-reset/by-verification/send-code")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetCode(
            @Valid @RequestBody SendSessionEmailCodeRequest request) {
        findAccountService.sendPasswordResetCode(request.sessionId(), request.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
    }

    @Operation(summary = "비밀번호 재설정 - OTP 확인",
            description = "OTP를 확인하고 재설정 토큰을 발급합니다. 성공 시 본인인증(CI)이 계정에 자동 연결됩니다.")
    @PostMapping("/password-reset/by-verification/confirm")
    public ApiResponse<PasswordResetTokenResponse> confirmPasswordReset(
            @Valid @RequestBody ConfirmSessionRequest request) {
        return ApiResponse.ok(findAccountService.confirmPasswordReset(request.sessionId(), request.code()));
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
