package com.thinh.smartgym.auth.dto;

import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Thông tin tài khoản hội viên vừa được tạo")
public record RegisterResponse(
        @Schema(example = "101")
        Long id,
        @Schema(example = "Nguyen Van An")
        String fullName,
        @Schema(example = "user@gmail.com")
        String email,
        @Schema(example = "ROLE_MEMBER")
        RoleName role,
        @Schema(example = "ACTIVE")
        AccountStatus accountStatus,
        @Schema(type = "string", format = "date-time", example = "2026-07-31T08:00:00Z")
        Instant createdAt
) {
}
