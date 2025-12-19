package com.retrip.auth.infra.adapter.in.rest.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private final boolean success;
    private final int status;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> ok(T data) {
        return success(data, HttpStatus.OK);
    }

    public static <T> ApiResponse<T> created(T data) {
        return success(data, HttpStatus.CREATED);
    }

    public static <T> ApiResponse<T> noContent() {
        return success(null, HttpStatus.NO_CONTENT);
    }

    public static <T> ApiResponse<T> of(T data, HttpStatus status) {
        return success(data, status);
    }

    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    private static <T> ApiResponse<T> success(T data, HttpStatus status) {
        return new ApiResponse<>(true, status.value(), status.getReasonPhrase(), data);
    }

    public static ApiResponse<ErrorResponse> of(ErrorResponse errorResponse) {
        return new ApiResponse<>(
                false,
                errorResponse.getStatus(),
                HttpStatus.valueOf(errorResponse.getStatus()).getReasonPhrase(),
                errorResponse
        );
    }
}