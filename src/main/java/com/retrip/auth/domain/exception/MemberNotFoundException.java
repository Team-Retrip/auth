package com.retrip.auth.domain.exception;

import com.retrip.auth.domain.exception.common.EntityNotFoundException;
import com.retrip.auth.domain.exception.common.ErrorCode;

public class MemberNotFoundException extends EntityNotFoundException {
    private static final ErrorCode errorCode = ErrorCode.MEMBER_NOT_FOUND;

    public MemberNotFoundException() {
        super(errorCode);
    }
}

