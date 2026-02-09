package com.retrip.auth.domain.exception;

import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;

public class DuplicateUserException extends BusinessException {
    public DuplicateUserException() {
        super(ErrorCode.DUPLICATE_USER);
    }
}
