package com.thinh.smartgym.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.common.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestAuthenticationEntryPoint authenticationEntryPoint;
    private RestAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        authenticationEntryPoint = new RestAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new RestAccessDeniedHandler(objectMapper);
    }

    @Test
    @DisplayName("Phản hồi 401 tuân thủ ErrorResponse và mã ACC-005")
    void authenticationEntryPoint_ShouldReturnStandardErrorResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/member/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response, new BadCredentialsException("invalid token"));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("errorCode").asText()).isEqualTo("ACC-005");
        assertThat(body.get("message").asText()).isNotBlank();
        assertThat(body.get("details").isObject()).isTrue();
        assertThat(body.has("status")).isFalse();
        assertThat(body.has("path")).isFalse();
    }

    @Test
    @DisplayName("Phản hồi 403 cho LOCKED dùng mã ACC-004 và details.accountStatus")
    void accessDeniedHandler_WithLockedAccount_ShouldReturnAcc004() throws Exception {
        assertAccountStatusResponse(AccountStatus.LOCKED, "ACC-004");
    }

    @Test
    @DisplayName("Phản hồi 403 cho DISABLED dùng mã ACC-006 và details.accountStatus")
    void accessDeniedHandler_WithDisabledAccount_ShouldReturnAcc006() throws Exception {
        assertAccountStatusResponse(AccountStatus.DISABLED, "ACC-006");
    }

    private void assertAccountStatusResponse(AccountStatus status, String expectedErrorCode) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccountStatusAccessDeniedException(status));

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("errorCode").asText()).isEqualTo(expectedErrorCode);
        assertThat(body.get("details").get("accountStatus").asText()).isEqualTo(status.name());
        assertThat(body.has("status")).isFalse();
        assertThat(body.has("path")).isFalse();
    }
}
