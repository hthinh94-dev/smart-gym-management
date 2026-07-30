package com.thinh.smartgym.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import com.thinh.smartgym.security.CustomUserDetailsService;
import com.thinh.smartgym.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "application.security.jwt.access-token-expiration-ms=3600000")
@AutoConfigureMockMvc
@Transactional
class AuthLoginIntegrationTest {

    private static final String PASSWORD = "SecurePass1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    @Value("${application.security.jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("Login issues a JWT with configured claims and users/me returns the principal")
    void login_WithValidCredentials_ShouldIssueJwtAndResolveCurrentUser() throws Exception {
        String email = registerUser();

        JsonNode loginBody = login(email.toUpperCase(), PASSWORD, 200);
        String token = loginBody.at("/data/accessToken").asText();

        assertThat(loginBody.at("/data/tokenType").asText()).isEqualTo("Bearer");
        assertThat(loginBody.at("/data/expiresIn").asLong()).isEqualTo(3600L);
        assertThat(loginBody.at("/data/user/email").asText()).isEqualTo(email);
        assertThat(loginBody.at("/data/user/role").asText()).isEqualTo("ROLE_MEMBER");
        assertThat(loginBody.at("/data/password").isMissingNode()).isTrue();
        assertThat(loginBody.at("/data/passwordHash").isMissingNode()).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo(email);

        List<?> roles = jwtService.extractClaim(token, claims -> claims.get("roles", List.class));
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst()).isEqualTo("ROLE_MEMBER");

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("ROLE_MEMBER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Unknown email and wrong password share ACC-007 without issuing a token")
    void login_WithInvalidCredentials_ShouldReturnSameAcc007Contract() throws Exception {
        String email = registerUser();

        assertInvalidCredentials("missing-" + email, PASSWORD);
        assertInvalidCredentials(email, "WrongPassword1");
    }

    @Test
    @DisplayName("Login maps LOCKED and DISABLED accounts to their business codes")
    void login_WithInactiveAccount_ShouldReturnStatusSpecificCode() throws Exception {
        String lockedEmail = registerUser();
        updateStatus(lockedEmail, AccountStatus.LOCKED);
        JsonNode lockedBody = login(lockedEmail, PASSWORD, 403);

        String disabledEmail = registerUser();
        updateStatus(disabledEmail, AccountStatus.DISABLED);
        JsonNode disabledBody = login(disabledEmail, PASSWORD, 403);

        assertThat(lockedBody.path("errorCode").asText()).isEqualTo("ACC-004");
        assertThat(lockedBody.at("/details/accountStatus").asText()).isEqualTo("LOCKED");
        assertThat(disabledBody.path("errorCode").asText()).isEqualTo("ACC-006");
        assertThat(disabledBody.at("/details/accountStatus").asText()).isEqualTo("DISABLED");
        assertThat(lockedBody.at("/data/accessToken").isMissingNode()).isTrue();
        assertThat(disabledBody.at("/data/accessToken").isMissingNode()).isTrue();
    }

    @Test
    @DisplayName("Missing, malformed and expired tokens return ACC-005")
    void currentUser_WithInvalidToken_ShouldReturnAcc005() throws Exception {
        assertInvalidJwt(null);
        assertInvalidJwt("Bearer malformed.jwt.token");

        String email = registerUser();
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal)
                userDetailsService.loadUserByUsername(email);
        JwtService expiredJwtService = expiredJwtService();
        assertInvalidJwt("Bearer " + expiredJwtService.generateAccessToken(principal));
    }

    @Test
    @DisplayName("AccountStatusGuard blocks an existing token after the account is locked")
    void currentUser_WithPreviouslyIssuedTokenAfterLock_ShouldReturnAcc004() throws Exception {
        String email = registerUser();
        String token = login(email, PASSWORD, 200).at("/data/accessToken").asText();

        updateStatus(email, AccountStatus.LOCKED);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACC-004"))
                .andExpect(jsonPath("$.details.accountStatus").value("LOCKED"));
    }

    @Test
    @DisplayName("AccountStatusGuard blocks an existing token after the account is disabled")
    void currentUser_WithPreviouslyIssuedTokenAfterDisable_ShouldReturnAcc006() throws Exception {
        String email = registerUser();
        String token = login(email, PASSWORD, 200).at("/data/accessToken").asText();

        updateStatus(email, AccountStatus.DISABLED);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACC-006"))
                .andExpect(jsonPath("$.details.accountStatus").value("DISABLED"));
    }

    private String registerUser() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@smartgym.test";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "Login Test Member");
        payload.put("email", email);
        payload.put("password", PASSWORD);
        payload.put("confirmPassword", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated());
        return email;
    }

    private JsonNode login(String email, String password, int expectedStatus) throws Exception {
        Map<String, Object> payload = Map.of("email", "  " + email + "  ", "password", password);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void assertInvalidCredentials(String email, String password) throws Exception {
        JsonNode body = login(email, password, 401);
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("errorCode").asText()).isEqualTo("ACC-007");
        assertThat(body.at("/data/accessToken").isMissingNode()).isTrue();
    }

    private void assertInvalidJwt(String authorizationHeader) throws Exception {
        var request = get("/api/v1/users/me");
        if (authorizationHeader != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACC-005"));
    }

    private void updateStatus(String email, AccountStatus accountStatus) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setAccountStatus(accountStatus);
        userRepository.saveAndFlush(user);
        entityManager.clear();
    }

    private JwtService expiredJwtService() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", jwtSecret);
        ReflectionTestUtils.setField(service, "jwtExpiration", -1_000L);
        ReflectionTestUtils.invokeMethod(service, "initializeSigningKey");
        return service;
    }
}
