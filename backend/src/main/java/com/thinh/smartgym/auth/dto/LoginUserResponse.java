package com.thinh.smartgym.auth.dto;

import com.thinh.smartgym.common.enums.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin người dùng đi kèm kết quả đăng nhập")
public record LoginUserResponse(
        @Schema(example = "101")
        Long id,
        @Schema(example = "Nguyen Van An")
        String fullName,
        @Schema(example = "user@gmail.com")
        String email,
        @Schema(example = "ROLE_MEMBER")
        RoleName role
) {
}
