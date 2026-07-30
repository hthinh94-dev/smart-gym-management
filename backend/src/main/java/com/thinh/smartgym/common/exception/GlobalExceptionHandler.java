package com.thinh.smartgym.common.exception;

import com.thinh.smartgym.auth.dto.RegisterRequest;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.security.AccountStatusAccessDeniedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Set<String> PASSWORD_FIELDS = Set.of("password", "confirmPassword");

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), exception.getDetails()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        boolean registerPasswordOnly = exception.getBindingResult().getTarget() instanceof RegisterRequest
                && !fieldErrors.isEmpty()
                && fieldErrors.stream().allMatch(error -> PASSWORD_FIELDS.contains(error.getField()));
        ErrorCode errorCode = registerPasswordOnly ? ErrorCode.INVALID_PASSWORD : ErrorCode.VALIDATION_ERROR;

        Map<String, String> violations = new LinkedHashMap<>();
        fieldErrors.forEach(error -> violations.putIfAbsent(
                error.getField(),
                Objects.requireNonNullElse(error.getDefaultMessage(), "Giá trị không hợp lệ.")
        ));

        Map<String, Object> details = new LinkedHashMap<>();
        if (!fieldErrors.isEmpty()) {
            FieldError firstError = fieldErrors.getFirst();
            details.put("field", firstError.getField());
            details.put("constraint", violations.get(firstError.getField()));
            if (!PASSWORD_FIELDS.contains(firstError.getField()) && firstError.getRejectedValue() != null) {
                details.put("rejectedValue", firstError.getRejectedValue());
            }
        }
        details.put("violations", violations);

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> violations = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation -> violations.put(
                violation.getPropertyPath().toString(),
                violation.getMessage()
        ));

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), Map.of("violations", violations)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson() {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        Map<String, String> details = Map.of(
                "field", "requestBody",
                "constraint", "JSON request body không đúng định dạng."
        );
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), details));
    }

    @ExceptionHandler(AccountStatusAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccountStatusAccessDenied(
            AccountStatusAccessDeniedException exception
    ) {
        return ResponseEntity.status(403).body(ErrorResponse.of(
                exception.getErrorCode(),
                exception.getMessage(),
                Map.of("accountStatus", exception.getAccountStatus().name())
        ));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurityException(RuntimeException exception) {
        // Security filter handlers own the public 401/403 contract and account-status error codes.
        throw exception;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Unhandled application error type={}", exception.getClass().getSimpleName());
        ErrorCode errorCode = ErrorCode.INTERNAL_CONFIGURATION_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), Map.of()));
    }
}
