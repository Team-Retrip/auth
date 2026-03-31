package com.retrip.auth.domain.exception.common;

import static org.springframework.http.HttpStatus.*;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SERVER_ERROR(INTERNAL_SERVER_ERROR, "Common-001", "Server error"),
    INVALID_INPUT_VALUE(BAD_REQUEST, "Common-002", "Invalid input value"),
    HANDLE_ACCESS_DENIED(FORBIDDEN, "Common-003", "Access is denied"),
    ENTITY_NOT_FOUND(BAD_REQUEST, "Common-004", "Entity not found"),
    ILLEGAL_STATE(BAD_REQUEST, "Common-005", "Illegal state"),
    INVALID_ACCESS(FORBIDDEN, "Common-006","접근 권한이 존재하지 않습니다."),
    EXTERNAL_API_ERROR(INTERNAL_SERVER_ERROR, "Common-007", "외부 API 호출 중 오류가 발생했습니다."),

    MEMBER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Member-001", "멤버 엔티티를 찾을 수 없습니다."),
    PASSWORD_NOT_MATCH(HttpStatus.UNAUTHORIZED, "Member-002", "비밀 번호가 다릅니다."),
    DELETED_MEMBER_CANNOT_REJOIN(HttpStatus.BAD_REQUEST, "Member-003", "탈퇴한 이메일은 재가입할 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Member-004", "이미 존재하는 이메일입니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "Member-005", "이미 가입된 정보입니다."),

    EXTENSION_NOT_FOUND(BAD_REQUEST, "Image-001", "지원하지 않는 이미지 확장자입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
