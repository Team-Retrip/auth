package com.retrip.auth.domain.exception;

import com.retrip.auth.domain.exception.common.EntityNotFoundException;
import com.retrip.auth.domain.exception.common.ErrorCode;

public class PasswordNotMatchException extends EntityNotFoundException {
    private static final ErrorCode errorCode = ErrorCode.PASSWORD_NOT_MATCH;

    public PasswordNotMatchException() {
        super(errorCode);
    }
}

