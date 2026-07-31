package com.thinh.smartgym.auth.dto.admin;

import com.thinh.smartgym.common.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Kết quả mở khóa tài khoản trong phạm vi thao tác hiện tại")
public record UnlockUserResponse(
        @Schema(example = "102")
        Long userId,
        @Schema(example = "Tran Thi Binh")
        String fullName,
        @Schema(example = "ACTIVE")
        AccountStatus accountStatus,
        @Schema(description = "Email Admin thực hiện", example = "admin@smartgym.com")
        String unlockedBy,
        @Schema(type = "string", format = "date-time", example = "2026-07-31T10:00:00Z")
        Instant unlockedAt
) {
}
