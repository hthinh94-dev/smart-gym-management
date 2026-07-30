package com.thinh.smartgym.auth.dto.admin;

import com.thinh.smartgym.common.enums.AccountStatus;

import java.time.Instant;

public record LockUserResponse(
        Long userId,
        String fullName,
        AccountStatus accountStatus,
        String lockedBy,
        Instant lockedAt,
        String reason,
        String subscriptionStatus
) {
}
