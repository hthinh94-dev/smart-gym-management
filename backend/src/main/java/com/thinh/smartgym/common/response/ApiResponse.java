package com.thinh.smartgym.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Envelope response chuẩn cho API thành công")
public class ApiResponse<T> {

    @Builder.Default
    @Schema(example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean success = true;

    @Schema(example = "Xử lý yêu cầu thành công", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Payload theo từng endpoint", requiredMode = Schema.RequiredMode.REQUIRED)
    private T data;

    @Schema(hidden = true)
    private String errorCode;

    @Schema(hidden = true)
    private Object details;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, Object details) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .build();
    }
}
