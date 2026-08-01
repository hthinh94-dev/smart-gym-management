package com.thinh.smartgym.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.auth.dto.LoginRequest;
import com.thinh.smartgym.auth.dto.LoginResponse;
import com.thinh.smartgym.auth.dto.LoginUserResponse;
import com.thinh.smartgym.auth.service.AuthService;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.common.exception.GlobalExceptionHandler;
import com.thinh.smartgym.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST login trả 200 và response không có password")
    void login_WithValidPayload_ShouldReturnTokenWithoutPassword() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(successResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", "  MEMBER@SMARTGYM.COM  ",
                                "password", "SecurePass1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.accessToken").value("signed-access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.email").value("member@smartgym.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Sai credentials trả 401 ACC-007")
    void login_WithInvalidCredentials_ShouldReturnAcc007() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        performValidLogin()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACC-007"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("LOCKED trả 403 ACC-004 với accountStatus")
    void login_WithLockedAccount_ShouldReturnAcc004() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BusinessException(
                ErrorCode.ACCOUNT_LOCKED,
                Map.of("accountStatus", "LOCKED")
        ));

        performValidLogin()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACC-004"))
                .andExpect(jsonPath("$.details.accountStatus").value("LOCKED"));
    }

    @Test
    @DisplayName("DISABLED trả 403 ACC-006 với accountStatus")
    void login_WithDisabledAccount_ShouldReturnAcc006() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BusinessException(
                ErrorCode.ACCOUNT_DISABLED,
                Map.of("accountStatus", "DISABLED")
        ));

        performValidLogin()
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACC-006"))
                .andExpect(jsonPath("$.details.accountStatus").value("DISABLED"));
    }

    @Test
    @DisplayName("Password rỗng của Login trả VAL-001, không dùng ACC-002 của Register")
    void login_WithBlankPassword_ShouldReturnVal001() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", "member@smartgym.com",
                                "password", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations.password").exists());
    }

    private org.springframework.test.web.servlet.ResultActions performValidLogin() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of(
                        "email", "member@smartgym.com",
                        "password", "SecurePass1"
                ))));
    }

    private LoginResponse successResponse() {
        return new LoginResponse(
                "signed-access-token",
                "Bearer",
                3600,
                new LoginUserResponse(101L, "Gym Member", "member@smartgym.com", RoleName.ROLE_MEMBER)
        );
    }
}
