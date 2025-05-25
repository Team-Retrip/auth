package com.retrip.auth.infra.adapter.in.rest.common;

import static org.springframework.http.HttpStatus.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private boolean success;
    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> created(T data) {
        return success(data, CREATED);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return success(data, OK);
    }

    public static <T> ApiResponse<T> noContent() {
        return success(null, NO_CONTENT);
    }

    public static <T> ApiResponse<T> of(T data, HttpStatus status) {
        return success(data, status);
    }

    private static <T> ApiResponse<T> success(T data, HttpStatus status) {
        return new ApiResponse<>(true, status.value(), status.getReasonPhrase(), data);
    }

    public static ApiResponse<ErrorResponse> of(ErrorResponse errorResponse) {
        return new ApiResponse<>(false, errorResponse.getStatus(), valueOf(errorResponse.getStatus()).getReasonPhrase(), errorResponse);
    }
}
