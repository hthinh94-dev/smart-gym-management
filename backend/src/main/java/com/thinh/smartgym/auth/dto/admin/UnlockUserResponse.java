package com.thinh.smartgym.auth.dto.admin;

import com.thinh.smartgym.common.enums.AccountStatus;

import java.time.Instant;

public record UnlockUserResponse(
        Long userId,
        String fullName,
        AccountStatus accountStatus,
        String unlockedBy,
        Instant unlockedAt
) {
}
