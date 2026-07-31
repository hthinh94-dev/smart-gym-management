package com.thinh.smartgym.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseContractTest {

    /** Kiểm tra factory thành công tối giản giữ đúng contract và không sinh trường lỗi. */
    @Test
    @DisplayName("ApiResponse success tao envelope thanh cong toi gian")
    void apiResponseSuccess_ShouldCreateSuccessfulEnvelope() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getDetails()).isNull();
    }

    /** Kiểm tra factory thành công có message giữ nguyên payload và nội dung thông báo. */
    @Test
    @DisplayName("ApiResponse success giu message va data")
    void apiResponseSuccess_WithMessage_ShouldPreserveMessageAndData() {
        ApiResponse<Integer> response = ApiResponse.success("Tạo thành công", 42);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Tạo thành công");
        assertThat(response.getData()).isEqualTo(42);
    }

    /** Kiểm tra factory lỗi không vô tình đánh dấu response là thành công. */
    @Test
    @DisplayName("ApiResponse error tao envelope that bai")
    void apiResponseError_ShouldCreateFailedEnvelope() {
        ApiResponse<Void> response = ApiResponse.error("ACC-007", "Thông tin đăng nhập không đúng");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("ACC-007");
        assertThat(response.getMessage()).isEqualTo("Thông tin đăng nhập không đúng");
        assertThat(response.getData()).isNull();
    }

    /** Kiểm tra error details được truyền nguyên vẹn cho client để xử lý theo field. */
    @Test
    @DisplayName("ApiResponse error giu nguyen details")
    void apiResponseError_WithDetails_ShouldPreserveDetails() {
        Map<String, String> details = Map.of("email", "Email không hợp lệ");

        ApiResponse<Void> response = ApiResponse.error("VAL-001", "Dữ liệu không hợp lệ", details);

        assertThat(response.getDetails()).isSameAs(details);
    }

    /** Kiểm tra ErrorResponse không trả details null vì frontend luôn kỳ vọng một object. */
    @Test
    @DisplayName("ErrorResponse chuan hoa details null thanh map rong")
    void errorResponse_WithNullDetails_ShouldNormalizeToEmptyMap() {
        ErrorResponse response = ErrorResponse.of("SYS-001", "Lỗi hệ thống", null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getDetails()).isEqualTo(Map.of());
    }

    /** Kiểm tra factory ErrorResponse tối giản vẫn cung cấp đầy đủ error code và details rỗng. */
    @Test
    @DisplayName("ErrorResponse toi gian dung contract loi")
    void errorResponseWithoutDetails_ShouldUseEmptyMap() {
        ErrorResponse response = ErrorResponse.of("ACC-004", "Tài khoản bị khóa");

        assertThat(response.getErrorCode()).isEqualTo("ACC-004");
        assertThat(response.getMessage()).isEqualTo("Tài khoản bị khóa");
        assertThat(response.getDetails()).isEqualTo(Map.of());
    }

    /** Kiểm tra mapping từ Spring Page không làm sai metadata phân trang. */
    @Test
    @DisplayName("PageResponse map dung content va metadata")
    void pageResponseFrom_ShouldMapPageMetadata() {
        PageImpl<String> page = new PageImpl<>(
                List.of("A", "B"),
                PageRequest.of(1, 2),
                5
        );

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("A", "B");
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(2);
    }

    /** Kiểm tra content được defensive copy để caller không sửa response sau khi tạo. */
    @Test
    @DisplayName("PageResponse tao defensive copy bat bien")
    void pageResponse_ShouldDefensivelyCopyContent() {
        List<String> mutableContent = new ArrayList<>(List.of("A"));
        PageResponse<String> response = new PageResponse<>(mutableContent, 1, 1, 0, 20);

        mutableContent.add("B");

        assertThat(response.content()).containsExactly("A");
        assertThatThrownBy(() -> response.content().add("C"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /** Kiểm tra content null bị từ chối ngay thay vì tạo response phân trang không hợp lệ. */
    @Test
    @DisplayName("PageResponse tu choi content null")
    void pageResponse_WithNullContent_ShouldThrowException() {
        assertThatThrownBy(() -> new PageResponse<>(null, 0, 0, 0, 20))
                .isInstanceOf(NullPointerException.class);
    }
}
