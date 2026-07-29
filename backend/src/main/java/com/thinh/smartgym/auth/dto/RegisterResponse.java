package com.thinh.smartgym.auth.dto;

import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Thông tin tài khoản hội viên vừa được tạo")
public record RegisterResponse(
        Long id,
        String fullName,
        String email,
        RoleName role,
        AccountStatus accountStatus,
        Instant createdAt
) {
}
