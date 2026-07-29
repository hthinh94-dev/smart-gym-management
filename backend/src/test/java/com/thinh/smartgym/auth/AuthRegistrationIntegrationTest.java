package com.thinh.smartgym.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "application.cors.allowed-origins=http://localhost:5173")
@AutoConfigureMockMvc
@Transactional
class AuthRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Register public luu User ACTIVE, ROLE_MEMBER va BCrypt tren MySQL")
    void register_ShouldPersistNormalizedActiveMemberWithoutPrivilegeEscalation() throws Exception {
        String email = uniqueEmail();
        Map<String, Object> payload = validPayload(email);
        payload.put("role", "ROLE_ADMIN");
        payload.put("accountStatus", "DISABLED");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("ROLE_MEMBER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.confirmPassword").doesNotExist());

        entityManager.clear();
        User savedUser = userRepository.findByEmailWithRolesIgnoreCase(email).orElseThrow();
        assertThat(savedUser.getFullName()).isEqualTo("Nguyen Van An");
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecurePass1");
        assertThat(passwordEncoder.matches("SecurePass1", savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getUserRoles())
                .extracting(userRole -> userRole.getRole().getName())
                .containsExactly(RoleName.ROLE_MEMBER);
    }

    @Test
    @DisplayName("Register trung email sau normalize tra 409 ACC-001 tren full application context")
    void register_WithNormalizedDuplicateEmail_ShouldReturnAcc001() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(validPayload(email))))
                .andExpect(status().isCreated());

        Map<String, Object> duplicatePayload = validPayload("  " + email.toUpperCase() + "  ");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(duplicatePayload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACC-001"))
                .andExpect(jsonPath("$.details.rejectedValue").value(email));
    }

    @Test
    @DisplayName("CORS preflight cho localhost frontend duoc SecurityFilterChain chap nhan")
    void registerCorsPreflight_FromConfiguredOrigin_ShouldBeAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"
                ))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    private Map<String, Object> validPayload(String email) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "  Nguyen Van An  ");
        payload.put("email", email);
        payload.put("password", "SecurePass1");
        payload.put("confirmPassword", "SecurePass1");
        return payload;
    }

    private String uniqueEmail() {
        return "register-" + UUID.randomUUID() + "@smartgym.test";
    }
}
