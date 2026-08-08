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
    ACCOUNT_LOCKED(
            "ACC-004",
            HttpStatus.FORBIDDEN,
            "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ."
    ),
    ACCOUNT_DISABLED(
            "ACC-006",
            HttpStatus.FORBIDDEN,
            "Tài khoản đã bị vô hiệu hóa vĩnh viễn. Vui lòng liên hệ ban quản trị."
    ),
    INVALID_CREDENTIALS(
            "ACC-007",
            HttpStatus.UNAUTHORIZED,
            "Tên đăng nhập hoặc mật khẩu không chính xác."
    ),
    PROFILE_NOT_FOUND(
            "PROF-001",
            HttpStatus.NOT_FOUND,
            "Hồ sơ thể trạng chưa được hoàn thiện."
    ),
    MEMBERSHIP_PACKAGE_NOT_FOUND(
            "SUB-002",
            HttpStatus.NOT_FOUND,
            "Không tìm thấy gói tập."
    ),
    MEMBERSHIP_PACKAGE_INACTIVE(
            "SUB-003",
            HttpStatus.CONFLICT,
            "Gói tập đã ngừng hoạt động và không thể đăng ký."
    ),
    ACTIVE_SUBSCRIPTION_ALREADY_EXISTS(
            "SUB-004",
            HttpStatus.CONFLICT,
            "Bạn đang có gói tập ACTIVE và không thể đăng ký gói mới."
    ),
    SUBSCRIPTION_NOT_FOUND(
            "SUB-005",
            HttpStatus.NOT_FOUND,
            "Không tìm thấy Subscription hiện hành của hội viên."
    ),
    PENDING_SUBSCRIPTION_ALREADY_EXISTS(
            "SUB-006",
            HttpStatus.CONFLICT,
            "Bạn đã có yêu cầu đăng ký gói tập đang chờ xử lý."
    ),
    MEMBERSHIP_PACKAGE_NAME_ALREADY_EXISTS(
            "SUB-007",
            HttpStatus.CONFLICT,
            "Tên gói tập đã tồn tại."
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
