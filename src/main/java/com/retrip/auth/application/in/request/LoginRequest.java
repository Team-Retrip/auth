package com.retrip.auth.application.in.request;

public record LoginRequest(
        String id,       // email -> id로 변경 (JSON의 "id" 필드와 매핑됨)
        String password
) {
}