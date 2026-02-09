package com.retrip.auth.domain.exception;

import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;

public class PortOneApiException extends BusinessException {
    public PortOneApiException(String message) {
        super(ErrorCode.EXTERNAL_API_ERROR, message);
    }
}
