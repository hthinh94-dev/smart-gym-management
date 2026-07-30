package com.thinh.smartgym.auth.dto;

import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;

import java.time.Instant;

public record CurrentUserResponse(
        Long id,
        String fullName,
        String email,
        RoleName role,
        AccountStatus accountStatus,
        Instant createdAt
) {
}
