package com.thinh.smartgym.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envelope lỗi chuẩn; không chứa stack trace, SQL hoặc tên class Java")
public class ErrorResponse {

    @Builder.Default
    @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean success = false;

    @Schema(example = "VAL-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String errorCode;

    @Schema(example = "Dữ liệu đầu vào không hợp lệ.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Builder.Default
    @Schema(
            description = "Thông tin field, constraint hoặc trạng thái liên quan",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Object details = Map.of();

    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .details(Map.of())
                .build();
    }

    public static ErrorResponse of(String errorCode, String message, Object details) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .details(details == null ? Map.of() : details)
                .build();
    }
}
