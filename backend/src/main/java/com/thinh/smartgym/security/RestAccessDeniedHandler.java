package com.thinh.smartgym.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.common.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Lớp xử lý ngoại lệ 403 Forbidden khi người dùng đã đăng nhập nhưng không có đủ quyền truy cập tài nguyên.
 * Ghi đè hoàn toàn phản hồi mặc định của Spring Security bằng dữ liệu JSON chuẩn hóa.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ErrorResponse body;
        if (accessDeniedException instanceof AccountStatusAccessDeniedException accountStatusException) {
            body = ErrorResponse.of(
                    accountStatusException.getErrorCode(),
                    accountStatusException.getMessage(),
                    Map.of("accountStatus", accountStatusException.getAccountStatus().name())
            );
        } else {
            body = ErrorResponse.of(
                    "AUTH-002",
                    "Tài khoản của bạn không có đủ quyền hạn để truy cập tài nguyên này.",
                    Map.of()
            );
        }

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
