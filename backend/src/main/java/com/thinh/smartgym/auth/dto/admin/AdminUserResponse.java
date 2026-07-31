package com.thinh.smartgym.auth.dto.admin;

import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Tài khoản hiển thị trong danh sách quản trị")
public record AdminUserResponse(
        @Schema(example = "102")
        Long id,
        @Schema(example = "Tran Thi Binh")
        String fullName,
        @Schema(example = "member@gmail.com")
        String email,
        @Schema(example = "ROLE_MEMBER")
        RoleName role,
        @Schema(example = "ACTIVE")
        AccountStatus accountStatus,
        @Schema(type = "string", format = "date-time", example = "2026-07-30T08:00:00Z")
        Instant createdAt,
        @Schema(description = "Có subscription ACTIVE và còn hiệu lực trong ngày nghiệp vụ", example = "true")
        boolean hasActiveSubscription
) {
}
