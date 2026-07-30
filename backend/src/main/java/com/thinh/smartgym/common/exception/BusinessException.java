package com.thinh.smartgym.common.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final Object details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    public BusinessException(ErrorCode errorCode, Object details) {
        this(errorCode, errorCode.getMessage(), details);
    }

    public BusinessException(ErrorCode errorCode, String message, Object details) {
        super(message == null || message.isBlank() ? errorCode.getMessage() : message);
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}
