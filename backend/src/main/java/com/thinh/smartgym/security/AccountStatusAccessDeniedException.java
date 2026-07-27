package com.thinh.smartgym.security;

import com.thinh.smartgym.common.enums.AccountStatus;
import org.springframework.security.access.AccessDeniedException;

public class AccountStatusAccessDeniedException extends AccessDeniedException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final AccountStatus accountStatus;

    public AccountStatusAccessDeniedException(AccountStatus accountStatus) {
        super(messageFor(accountStatus));
        this.errorCode = errorCodeFor(accountStatus);
        this.accountStatus = accountStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    private static String errorCodeFor(AccountStatus accountStatus) {
        return switch (accountStatus) {
            case LOCKED -> "ACC-004";
            case DISABLED -> "ACC-006";
            case ACTIVE -> throw new IllegalArgumentException("ACTIVE accounts must not be denied");
        };
    }

    private static String messageFor(AccountStatus accountStatus) {
        return switch (accountStatus) {
            case LOCKED -> "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.";
            case DISABLED -> "Tài khoản đã bị vô hiệu hóa vĩnh viễn. Vui lòng liên hệ ban quản trị.";
            case ACTIVE -> throw new IllegalArgumentException("ACTIVE accounts must not be denied");
        };
    }
}
