package com.thinh.smartgym.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Schema(description = "Dữ liệu đăng nhập bằng email và mật khẩu")
public class LoginRequest {

    @NotBlank(message = "Email là bắt buộc.")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
    @Schema(example = "user@gmail.com", maxLength = 150)
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, example = "SecurePass1")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        setEmail(email);
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
