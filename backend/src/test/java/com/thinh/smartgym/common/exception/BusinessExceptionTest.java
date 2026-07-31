package com.thinh.smartgym.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    /** Kiểm tra constructor tối giản lấy message chuẩn và details rỗng từ ErrorCode. */
    @Test
    @DisplayName("BusinessException toi gian dung message mac dinh")
    void constructor_WithErrorCode_ShouldUseDefaultContract() {
        BusinessException exception = new BusinessException(ErrorCode.INVALID_CREDENTIALS);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getMessage());
        assertThat(exception.getDetails()).isEqualTo(Map.of());
    }

    /** Kiểm tra custom details được giữ nguyên để GlobalExceptionHandler trả đúng ngữ cảnh nghiệp vụ. */
    @Test
    @DisplayName("BusinessException giu nguyen details nghiep vu")
    void constructor_WithDetails_ShouldPreserveDetails() {
        Map<String, String> details = Map.of("accountStatus", "LOCKED");

        BusinessException exception = new BusinessException(ErrorCode.ACCOUNT_LOCKED, details);

        assertThat(exception.getDetails()).isSameAs(details);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.ACCOUNT_LOCKED.getMessage());
    }

    /** Kiểm tra custom message hợp lệ được ưu tiên cho trường hợp cần thông báo chuyên biệt. */
    @Test
    @DisplayName("BusinessException uu tien custom message hop le")
    void constructor_WithCustomMessage_ShouldUseCustomMessage() {
        BusinessException exception = new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                "Lý do khóa chưa hợp lệ",
                Map.of("reason", "Tối thiểu 10 ký tự")
        );

        assertThat(exception.getMessage()).isEqualTo("Lý do khóa chưa hợp lệ");
    }

    /** Kiểm tra message null hoặc chỉ có khoảng trắng không làm mất thông báo chuẩn. */
    @Test
    @DisplayName("BusinessException fallback khi custom message rong")
    void constructor_WithBlankMessage_ShouldFallbackToErrorCodeMessage() {
        BusinessException nullMessage = new BusinessException(ErrorCode.VALIDATION_ERROR, null, null);
        BusinessException blankMessage = new BusinessException(ErrorCode.VALIDATION_ERROR, "   ", null);

        assertThat(nullMessage.getMessage()).isEqualTo(ErrorCode.VALIDATION_ERROR.getMessage());
        assertThat(blankMessage.getMessage()).isEqualTo(ErrorCode.VALIDATION_ERROR.getMessage());
        assertThat(nullMessage.getDetails()).isEqualTo(Map.of());
        assertThat(blankMessage.getDetails()).isEqualTo(Map.of());
    }

    /** Kiểm tra registry mã lỗi không trùng code và luôn có HTTP status/message đầy đủ. */
    @Test
    @DisplayName("ErrorCode registry co code duy nhat va metadata day du")
    void errorCodeRegistry_ShouldContainUniqueCompleteContracts() {
        ErrorCode[] values = ErrorCode.values();

        assertThat(Arrays.stream(values).map(ErrorCode::getCode))
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrder(
                        "ACC-001", "ACC-002", "ACC-004", "ACC-006",
                        "ACC-007", "VAL-001", "SYS-001"
                );
        assertThat(values).allSatisfy(errorCode -> {
            assertThat(errorCode.getHttpStatus()).isNotNull();
            assertThat(errorCode.getMessage()).isNotBlank();
        });
    }
}
