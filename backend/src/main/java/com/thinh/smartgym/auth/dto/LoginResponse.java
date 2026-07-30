package com.thinh.smartgym.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token và thông tin người dùng đăng nhập")
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        LoginUserResponse user
) {
}
