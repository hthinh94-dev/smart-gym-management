package com.thinh.smartgym.auth.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LockUserRequest(
        @NotBlank(message = "Lý do khóa tài khoản là bắt buộc.")
        @Size(min = 10, max = 500, message = "Lý do khóa phải có từ 10 đến 500 ký tự.")
        @Schema(
                example = "Vi phạm nội quy phòng tập: gây mất trật tự nghiêm trọng.",
                minLength = 10,
                maxLength = 500
        )
        String reason
) {

    public LockUserRequest {
        reason = reason == null ? null : reason.trim();
    }
}
