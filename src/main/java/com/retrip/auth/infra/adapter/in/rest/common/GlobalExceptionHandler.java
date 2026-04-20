package com.retrip.auth.infra.adapter.in.rest.common;

import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<ErrorResponse> handleAccessDeniedException(HttpServletRequest request, AccessDeniedException e) {
        log.error("handleAccessDeniedException: ", e);
        return handle(ErrorCode.HANDLE_ACCESS_DENIED, request);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<ErrorResponse> handleBindException(HttpServletRequest request, BindException e) {
        log.error("handleBindException: ", e);
        return ApiResponse.of(
                ErrorResponse.of(
                        ErrorCode.INVALID_INPUT_VALUE,
                        request.getRequestURL().toString(),
                        request.getMethod(),
                        e.getBindingResult()
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<ErrorResponse> handleBusinessException(HttpServletRequest request, BusinessException e) {
        log.error("handleBusinessException: ", e);
        return handle(e.getErrorCode(), request);
    }


    @ExceptionHandler(MailException.class)
    public ApiResponse<ErrorResponse> handleMailException(HttpServletRequest request, MailException e) {
        log.error("handleMailException: ", e);
        return handle(ErrorCode.MAIL_SEND_FAILED, request);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<ErrorResponse> handleException(HttpServletRequest request, Exception e) {
        log.error("handleException: ", e);
        return handle(ErrorCode.SERVER_ERROR, request);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ApiResponse<ErrorResponse> handleBadCredentialsException(HttpServletRequest request, BadCredentialsException e) {
        log.error("handleBadCredentialsException: ", e);
        return ApiResponse.of(
                ErrorResponse.of(
                        ErrorCode.MEMBER_NOT_FOUND, // 401 상태 코드 (Member-001)
                        request.getRequestURL().toString(),
                        request.getMethod()
                ));
    }

    private static ApiResponse<ErrorResponse> handle(ErrorCode errorCode, HttpServletRequest request) {
        return ApiResponse.of(
                ErrorResponse.of(
                        errorCode, request.getRequestURL().toString(), request.getMethod()
                ));
    }
}
