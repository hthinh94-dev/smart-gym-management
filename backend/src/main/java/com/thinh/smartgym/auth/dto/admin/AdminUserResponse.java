package com.thinh.smartgym.auth.dto.admin;

import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        RoleName role,
        AccountStatus accountStatus,
        Instant createdAt,
        boolean hasActiveSubscription
) {
}
