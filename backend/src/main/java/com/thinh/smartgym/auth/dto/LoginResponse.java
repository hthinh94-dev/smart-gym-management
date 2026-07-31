package com.thinh.smartgym.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token và thông tin người dùng đăng nhập")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9.payload.signature")
        String accessToken,
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(description = "Thời hạn token theo giây, lấy từ cấu hình", example = "3600")
        long expiresIn,
        LoginUserResponse user
) {
}
