package com.thinh.smartgym.auth.dto;

import com.thinh.smartgym.common.enums.RoleName;

public record LoginUserResponse(
        Long id,
        String fullName,
        String email,
        RoleName role
) {
}
