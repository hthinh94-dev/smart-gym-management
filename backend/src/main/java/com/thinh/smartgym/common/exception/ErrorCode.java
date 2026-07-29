package com.thinh.smartgym.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    EMAIL_ALREADY_EXISTS(
            "ACC-001",
            HttpStatus.CONFLICT,
            "Email này đã được sử dụng bởi một tài khoản khác trong hệ thống."
    ),
    INVALID_PASSWORD(
            "ACC-002",
            HttpStatus.BAD_REQUEST,
            "Mật khẩu không đáp ứng yêu cầu bảo mật."
    ),
    VALIDATION_ERROR(
            "VAL-001",
            HttpStatus.BAD_REQUEST,
            "Dữ liệu đầu vào không hợp lệ."
    ),
    INTERNAL_CONFIGURATION_ERROR(
            "SYS-001",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Hệ thống chưa được cấu hình đầy đủ. Vui lòng liên hệ quản trị viên."
    );

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
