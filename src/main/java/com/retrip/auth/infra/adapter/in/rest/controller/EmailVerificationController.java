package com.retrip.auth.infra.adapter.in.rest.controller;

import com.retrip.auth.application.dto.request.SendEmailCodeRequest;
import com.retrip.auth.application.dto.request.VerifyEmailCodeRequest;
import com.retrip.auth.application.service.EmailVerificationService;
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
@RequestMapping("/auth/email")
@RequiredArgsConstructor
@Tag(name = "이메일 인증", description = "회원가입 이메일 인증 API")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "인증코드 발송",
            description = "회원가입용 이메일 인증코드를 발송합니다. 이미 가입된 이메일은 거부됩니다. 코드는 10분간 유효합니다.")
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(
            @Valid @RequestBody SendEmailCodeRequest request) {
        emailVerificationService.sendSignupCode(request.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
    }

    @Operation(summary = "인증코드 확인",
            description = "발송된 인증코드를 확인합니다. 확인 후 30분 이내에 회원가입을 완료해야 합니다.")
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @Valid @RequestBody VerifyEmailCodeRequest request) {
        emailVerificationService.verifySignupCode(request.email(), request.code());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
    }
}
