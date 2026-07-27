package com.thinh.smartgym.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;

    private String errorCode;

    private String message;

    private Object details;

    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    public static ErrorResponse of(String errorCode, String message, Object details) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .build();
    }
}
