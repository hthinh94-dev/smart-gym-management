package com.thinh.smartgym.common.exception;

import com.thinh.smartgym.auth.dto.RegisterRequest;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.security.AccountStatusAccessDeniedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @Mock
    private Path propertyPath;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    /** Kiểm tra BusinessException được ánh xạ nguyên vẹn sang HTTP status và error contract. */
    @Test
    @DisplayName("Handler map BusinessException dung status va details")
    void handleBusinessException_ShouldMapBusinessContract() {
        BusinessException exception = new BusinessException(
                ErrorCode.EMAIL_ALREADY_EXISTS,
                Map.of("field", "email")
        );

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACC-001");
        assertThat(response.getBody().getDetails()).isEqualTo(Map.of("field", "email"));
    }

    /** Kiểm tra lỗi password đăng ký dùng ACC-002 và tuyệt đối không trả rejected password. */
    @Test
    @DisplayName("Handler che password va tra ACC-002")
    void handleMethodArgumentNotValid_WithPasswordOnly_ShouldMaskRejectedValue() {
        RegisterRequest target = new RegisterRequest(
                "Nguyễn Văn An",
                "member@smartgym.com",
                "weak",
                "weak"
        );
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "registerRequest");
        bindingResult.addError(new FieldError(
                "registerRequest",
                "password",
                "weak",
                false,
                null,
                null,
                "Mật khẩu không hợp lệ"
        ));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACC-002");
        assertThat(response.getBody().getDetails().toString()).doesNotContain("weak", "rejectedValue");
    }

    /** Kiểm tra validation field thông thường dùng VAL-001 và giữ rejected value hữu ích. */
    @Test
    @DisplayName("Handler tra VAL-001 va rejectedValue cho field thuong")
    void handleMethodArgumentNotValid_WithRegularField_ShouldIncludeRejectedValue() {
        RegisterRequest target = new RegisterRequest("", "member@smartgym.com", "SecurePass1", "SecurePass1");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "registerRequest");
        bindingResult.addError(new FieldError(
                "registerRequest",
                "fullName",
                "",
                false,
                null,
                null,
                null
        ));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VAL-001");
        assertThat(response.getBody().getDetails().toString())
                .contains("fullName", "Giá trị không hợp lệ", "rejectedValue");
    }

    /** Kiểm tra ConstraintViolationException gom đúng property path và message theo field. */
    @Test
    @DisplayName("Handler gom constraint violations theo property path")
    void handleConstraintViolation_ShouldMapViolations() {
        when(constraintViolation.getPropertyPath()).thenReturn(propertyPath);
        when(propertyPath.toString()).thenReturn("lockUser.reason");
        when(constraintViolation.getMessage()).thenReturn("Lý do phải có ít nhất 10 ký tự");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(constraintViolation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VAL-001");
        assertThat(response.getBody().getDetails().toString())
                .contains("lockUser.reason", "Lý do phải có ít nhất 10 ký tự");
    }

    /** Kiểm tra JSON hỏng trả lỗi requestBody ổn định, không lộ parser exception. */
    @Test
    @DisplayName("Handler map JSON khong doc duoc thanh VAL-001")
    void handleUnreadableJson_ShouldReturnSanitizedValidationError() {
        ResponseEntity<ErrorResponse> response = handler.handleUnreadableJson();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VAL-001");
        assertThat(response.getBody().getDetails().toString()).contains("requestBody", "JSON");
    }

    /** Kiểm tra query parameter sai type trả field/value để frontend hiển thị đúng lỗi. */
    @Test
    @DisplayName("Handler map query parameter sai type")
    void handleTypeMismatch_WithRejectedValue_ShouldIncludeValue() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Integer.class,
                "page",
                methodParameter,
                new NumberFormatException("not a number")
        );

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatch(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails().toString()).contains("page", "abc");
    }

    /** Kiểm tra query parameter null không tạo rejectedValue dư thừa trong response. */
    @Test
    @DisplayName("Handler bo rejectedValue khi query value null")
    void handleTypeMismatch_WithNullValue_ShouldOmitRejectedValue() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                null,
                Integer.class,
                "page",
                methodParameter,
                null
        );

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatch(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails().toString()).contains("page").doesNotContain("rejectedValue");
    }

    /** Kiểm tra account status exception giữ ACC-004/ACC-006 và status trong details. */
    @Test
    @DisplayName("Handler map account status access denied")
    void handleAccountStatusAccessDenied_ShouldPreserveStatusContract() {
        ResponseEntity<ErrorResponse> response = handler.handleAccountStatusAccessDenied(
                new AccountStatusAccessDeniedException(AccountStatus.LOCKED)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACC-004");
        assertThat(response.getBody().getDetails()).isEqualTo(Map.of("accountStatus", "LOCKED"));
    }

    /** Kiểm tra security exceptions được trả lại cho filter handlers sở hữu contract 401/403. */
    @Test
    @DisplayName("Handler rethrow security exceptions")
    void rethrowSecurityException_ShouldPreserveOriginalException() {
        AccessDeniedException accessDenied = new AccessDeniedException("forbidden");
        BadCredentialsException badCredentials = new BadCredentialsException("unauthorized");

        assertThatThrownBy(() -> handler.rethrowSecurityException(accessDenied)).isSameAs(accessDenied);
        assertThatThrownBy(() -> handler.rethrowSecurityException(badCredentials)).isSameAs(badCredentials);
    }

    /** Kiểm tra exception bất ngờ chỉ trả SYS-001, không phát tán message nội bộ hoặc stack trace. */
    @Test
    @DisplayName("Handler an chi tiet exception bat ngo")
    void handleUnexpectedException_ShouldReturnSanitizedSystemError() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(
                new IllegalStateException("jdbc:mysql://secret-host/internal")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("SYS-001");
        assertThat(response.getBody().getDetails()).isEqualTo(Map.of());
        assertThat(response.getBody().toString()).doesNotContain("secret-host", "IllegalStateException");
    }
}
