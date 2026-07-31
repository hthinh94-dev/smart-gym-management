package com.thinh.smartgym.auth.dto;

import com.thinh.smartgym.auth.dto.admin.LockUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    /** Kiểm tra đăng ký chuẩn hóa khoảng trắng của tên/email nhưng không thay đổi password. */
    @Test
    @DisplayName("RegisterRequest chuan hoa identity va giu nguyen password")
    void registerRequest_ShouldNormalizeIdentityWithoutMutatingPassword() {
        RegisterRequest request = new RegisterRequest(
                "  Nguyễn Văn An  ",
                "  Member@SmartGym.com  ",
                " SecurePass1 ",
                " SecurePass1 "
        );

        assertThat(request.getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(request.getEmail()).isEqualTo("Member@SmartGym.com");
        assertThat(request.getPassword()).isEqualTo(" SecurePass1 ");
        assertThat(request.getConfirmPassword()).isEqualTo(" SecurePass1 ");
    }

    /** Kiểm tra password hợp lệ tại đúng hai biên bảo mật 8 và 72 ký tự. */
    @Test
    @DisplayName("RegisterRequest chap nhan password o bien 8 va 72 ky tu")
    void registerRequest_WithBoundaryPasswordLengths_ShouldBeValid() {
        RegisterRequest minimum = validRegisterRequest("Abcdefg1");
        RegisterRequest maximum = validRegisterRequest("A1" + "a".repeat(70));

        assertThat(validator.validate(minimum)).isEmpty();
        assertThat(validator.validate(maximum)).isEmpty();
    }

    /** Kiểm tra password ngoài biên, thiếu chữ hoa/số hoặc có whitespace biên đều bị chặn. */
    @Test
    @DisplayName("RegisterRequest tu choi cac password vi pham chinh sach")
    void registerRequest_WithInvalidPasswordPolicies_ShouldBeRejected() {
        assertPasswordViolation("Abcdef1");
        assertPasswordViolation("A1" + "a".repeat(71));
        assertPasswordViolation("abcdefgh1");
        assertPasswordViolation("Abcdefghi");
        assertPasswordViolation(" SecurePass1");
        assertPasswordViolation("SecurePass1 ");
    }

    /** Kiểm tra các trường identity trống và email sai định dạng phát sinh đúng field violations. */
    @Test
    @DisplayName("RegisterRequest bao loi field bat buoc va email sai dinh dang")
    void registerRequest_WithInvalidIdentityFields_ShouldExposeFieldViolations() {
        RegisterRequest request = new RegisterRequest("   ", "not-an-email", "SecurePass1", "SecurePass1");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("fullName", "email");
    }

    /** Kiểm tra giới hạn tối đa của họ tên và email để khớp độ dài cột database. */
    @Test
    @DisplayName("RegisterRequest tu choi fullName va email vuot gioi han database")
    void registerRequest_WithOversizedIdentityFields_ShouldBeRejected() {
        String oversizedEmail = "a".repeat(140) + "@example.com";
        RegisterRequest request = new RegisterRequest(
                "N".repeat(101),
                oversizedEmail,
                "SecurePass1",
                "SecurePass1"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("fullName", "email");
    }

    /** Kiểm tra LoginRequest chuẩn hóa email không phụ thuộc locale và giữ nguyên password bí mật. */
    @Test
    @DisplayName("LoginRequest chuan hoa email va giu nguyen password")
    void loginRequest_ShouldNormalizeEmailWithoutMutatingPassword() {
        LoginRequest request = new LoginRequest("  MEMBER@SMARTGYM.COM  ", " Password1 ");

        assertThat(request.getEmail()).isEqualTo("member@smartgym.com");
        assertThat(request.getPassword()).isEqualTo(" Password1 ");
        assertThat(validator.validate(request)).isEmpty();
    }

    /** Kiểm tra LoginRequest từ chối field trống và email vượt quá 150 ký tự. */
    @Test
    @DisplayName("LoginRequest tu choi field trong va email qua dai")
    void loginRequest_WithInvalidFields_ShouldBeRejected() {
        LoginRequest blankRequest = new LoginRequest("   ", "   ");
        LoginRequest oversizedEmail = new LoginRequest("a".repeat(151), "Password1");

        assertThat(validator.validate(blankRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email", "password");
        assertThat(validator.validate(oversizedEmail))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email");
    }

    /** Kiểm tra lý do khóa được trim và chấp nhận đúng hai biên 10/500 ký tự. */
    @Test
    @DisplayName("LockUserRequest trim va chap nhan bien 10 den 500 ky tu")
    void lockUserRequest_WithBoundaryLengths_ShouldBeValid() {
        LockUserRequest minimum = new LockUserRequest("  " + "a".repeat(10) + "  ");
        LockUserRequest maximum = new LockUserRequest("b".repeat(500));

        assertThat(minimum.reason()).hasSize(10);
        assertThat(validator.validate(minimum)).isEmpty();
        assertThat(validator.validate(maximum)).isEmpty();
    }

    /** Kiểm tra lý do khóa null, trống và ngoài biên đều không thể gửi xuống service. */
    @Test
    @DisplayName("LockUserRequest tu choi ly do trong hoac ngoai bien")
    void lockUserRequest_WithInvalidReason_ShouldBeRejected() {
        assertThat(validator.validate(new LockUserRequest(null))).isNotEmpty();
        assertThat(validator.validate(new LockUserRequest("   "))).isNotEmpty();
        assertThat(validator.validate(new LockUserRequest("a".repeat(9)))).isNotEmpty();
        assertThat(validator.validate(new LockUserRequest("a".repeat(501)))).isNotEmpty();
    }

    private RegisterRequest validRegisterRequest(String password) {
        return new RegisterRequest("Nguyễn Văn An", "member@smartgym.com", password, password);
    }

    private void assertPasswordViolation(String password) {
        assertThat(validator.validate(validRegisterRequest(password)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password");
    }
}
