package com.thinh.smartgym.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Dữ liệu đăng ký tài khoản hội viên")
public class RegisterRequest {

    @NotBlank(message = "Họ tên là bắt buộc.")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự.")
    @Schema(example = "Nguyen Van An", maxLength = 100)
    private String fullName;

    @NotBlank(message = "Email là bắt buộc.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
    @Schema(example = "user@gmail.com", maxLength = 150)
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc.")
    @Pattern(
            regexp = "^(?!\\s)(?!.*\\s$)(?=.*[A-Z])(?=.*\\d).{8,72}$",
            message = "Mật khẩu phải từ 8 đến 72 ký tự, chứa ít nhất 1 chữ hoa và 1 chữ số, "
                    + "không có khoảng trắng ở đầu hoặc cuối."
    )
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 8, maxLength = 72, example = "SecurePass1")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu là bắt buộc.")
    @Size(max = 72, message = "Xác nhận mật khẩu không được vượt quá 72 ký tự.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, maxLength = 72, example = "SecurePass1")
    private String confirmPassword;

    public RegisterRequest() {
    }

    public RegisterRequest(String fullName, String email, String password, String confirmPassword) {
        setFullName(fullName);
        setEmail(email);
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
