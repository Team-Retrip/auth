package com.retrip.auth.infra.adapter.in.rest.common;

import com.retrip.auth.domain.exception.common.ErrorCode;

import java.util.ArrayList;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private String url = "Not available";
    private String method;
    private List<FieldError> errors = new ArrayList<>();

    private ErrorResponse(int status, String code, String message, String url, String method) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.url = url;
        this.method = method;
    }

    public static ErrorResponse of(ErrorCode code, String url, String method, BindingResult bindingResult) {
        return new ErrorResponse(code.getStatus().value(), code.getCode(), code.getMessage(), url, method, bindingResult.getFieldErrors());
    }

    public static ErrorResponse of(ErrorCode code, String url, String method) {
        return new ErrorResponse(code.getStatus().value(), code.getCode(), code.getMessage(), url, method);
    }
}
