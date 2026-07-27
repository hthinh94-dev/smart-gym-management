package com.thinh.smartgym.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.common.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Lớp xử lý ngoại lệ 401 Unauthorized khi người dùng chưa đăng nhập hoặc Token không hợp lệ.
 * Ghi đè hoàn toàn phản hồi mặc định của Spring Security bằng dữ liệu JSON chuẩn hóa.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse body = ErrorResponse.of(
                "ACC-005",
                "Token xác thực không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.",
                Map.of()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
