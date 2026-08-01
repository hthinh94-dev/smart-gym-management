package com.thinh.smartgym.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.auth.dto.RegisterRequest;
import com.thinh.smartgym.auth.dto.RegisterResponse;
import com.thinh.smartgym.auth.service.AuthService;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.common.exception.GlobalExceptionHandler;
import com.thinh.smartgym.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST register tra 201 va khong lo thong tin mat khau")
    void register_WithValidPayload_ShouldReturnCreatedWithoutPassword() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(successResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng ký tài khoản thành công"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_MEMBER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-07-28T08:00:00Z"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.confirmPassword").doesNotExist());
    }

    @Test
    @DisplayName("Email sai dinh dang tra VAL-001")
    void register_WithInvalidEmail_ShouldReturnVal001() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("email", "invalid-email");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations.email").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Email rong tra VAL-001")
    void register_WithBlankEmail_ShouldReturnVal001() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("email", "   ");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations.email").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Ho ten rong tra VAL-001")
    void register_WithBlankFullName_ShouldReturnVal001() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("fullName", "   ");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations.fullName").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Ho ten vuot qua 100 ky tu tra VAL-001")
    void register_WithTooLongFullName_ShouldReturnVal001() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("fullName", "A".repeat(101));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations.fullName").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Email vuot qua 150 ky tu tra VAL-001")
    void register_WithTooLongEmail_ShouldReturnVal001() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("email", "a".repeat(140) + "@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.violations.email").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @ParameterizedTest(name = "Password policy case {index}")
    @MethodSource("invalidPasswords")
    @DisplayName("Password sai policy tra ACC-002")
    void register_WithInvalidPassword_ShouldReturnAcc002(String invalidPassword) throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("password", invalidPassword);
        payload.put("confirmPassword", invalidPassword);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACC-002"))
                .andExpect(jsonPath("$.details.violations.password").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Confirm password khong khop tra ACC-002")
    void register_WithMismatchedConfirmation_ShouldReturnAcc002() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new BusinessException(
                ErrorCode.INVALID_PASSWORD,
                Map.of("field", "confirmPassword", "constraint", "Password confirmation does not match.")
        ));
        Map<String, Object> payload = validPayload();
        payload.put("confirmPassword", "DifferentPass1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ACC-002"))
                .andExpect(jsonPath("$.details.field").value("confirmPassword"));
    }

    @Test
    @DisplayName("Confirm password rong tra ACC-002")
    void register_WithBlankConfirmation_ShouldReturnAcc002() throws Exception {
        Map<String, Object> payload = validPayload();
        payload.put("confirmPassword", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ACC-002"))
                .andExpect(jsonPath("$.details.violations.confirmPassword").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Email trung tra 409 ACC-001 voi email da normalize")
    void register_WithDuplicateEmail_ShouldReturnAcc001() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new BusinessException(
                ErrorCode.EMAIL_ALREADY_EXISTS,
                Map.of("field", "email", "rejectedValue", "user@gmail.com")
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validPayload())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACC-001"))
                .andExpect(jsonPath("$.details.field").value("email"))
                .andExpect(jsonPath("$.details.rejectedValue").value("user@gmail.com"));
    }

    @Test
    @DisplayName("Thieu cau hinh ROLE_MEMBER tra 500 SYS-001 an toan")
    void register_WhenMemberRoleConfigurationIsMissing_ShouldReturnSys001() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(
                new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR)
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validPayload())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SYS-001"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("Client gui role va accountStatus van khong the gan quyen")
    void register_WithRoleAndStatusFields_ShouldIgnoreClientControlledValues() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(successResponse());
        Map<String, Object> payload = validPayload();
        payload.put("role", "ROLE_ADMIN");
        payload.put("accountStatus", "DISABLED");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ROLE_MEMBER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));

        ArgumentCaptor<RegisterRequest> requestCaptor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("User@Gmail.Com");
        assertThat(RegisterRequest.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("role", "accountStatus");
    }

    @Test
    @DisplayName("JSON sai dinh dang tra VAL-001 va khong lo parser exception")
    void register_WithMalformedJson_ShouldReturnVal001() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"user@gmail.com\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.field").value("requestBody"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    private RegisterResponse successResponse() {
        return new RegisterResponse(
                101L,
                "Nguyen Van An",
                "user@gmail.com",
                RoleName.ROLE_MEMBER,
                AccountStatus.ACTIVE,
                Instant.parse("2026-07-28T08:00:00Z")
        );
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "  Nguyen Van An  ");
        payload.put("email", "  User@Gmail.Com  ");
        payload.put("password", "SecurePass1");
        payload.put("confirmPassword", "SecurePass1");
        return payload;
    }

    private static Stream<String> invalidPasswords() {
        return Stream.of(
                "Pass1",
                "A1" + "a".repeat(71),
                "securepass1",
                "SecurePassword",
                " SecurePass1",
                "SecurePass1 "
        );
    }
}
