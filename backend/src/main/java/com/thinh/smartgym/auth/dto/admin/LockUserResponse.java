package com.thinh.smartgym.auth.dto.admin;

import com.thinh.smartgym.common.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Kết quả khóa tài khoản trong phạm vi thao tác hiện tại")
public record LockUserResponse(
        @Schema(example = "102")
        Long userId,
        @Schema(example = "Tran Thi Binh")
        String fullName,
        @Schema(example = "LOCKED")
        AccountStatus accountStatus,
        @Schema(description = "Email Admin thực hiện", example = "admin@smartgym.com")
        String lockedBy,
        @Schema(type = "string", format = "date-time", example = "2026-07-31T09:00:00Z")
        Instant lockedAt,
        @Schema(example = "Vi phạm nội quy phòng tập nhiều lần.")
        String reason,
        @Schema(example = "ACTIVE (không thay đổi)")
        String subscriptionStatus
) {
}
