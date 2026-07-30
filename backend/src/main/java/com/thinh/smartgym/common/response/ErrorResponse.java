package com.thinh.smartgym.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;

    private String errorCode;

    private String message;

    @Builder.Default
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
