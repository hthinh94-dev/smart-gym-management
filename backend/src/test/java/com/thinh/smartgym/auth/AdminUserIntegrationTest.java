package com.thinh.smartgym.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.RoleRepository;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "application.security.jwt.access-token-expiration-ms=3600000")
@AutoConfigureMockMvc
@Transactional
@Import(AdminUserIntegrationTest.FixedBusinessClockConfiguration.class)
class AdminUserIntegrationTest {

    private static final String PASSWORD = "SecurePass1";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-30T08:15:30Z");
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Admin list hỗ trợ page, search, role/status và biên ngày subscription")
    void getUsers_WithFilters_ShouldReturnCorrectPageAndSubscriptionBoundary() throws Exception {
        User admin = registerUser("Admin Local", uniqueEmail("admin"));
        assignAdminRole(admin);
        String adminToken = login(admin.getEmail(), PASSWORD, 200).at("/data/accessToken").asText();

        User activeMember = registerUser("Boundary Active Member", uniqueEmail("active-boundary"));
        User expiredBoundaryMember = registerUser("Boundary Ended Member", uniqueEmail("ended-boundary"));
        createSubscription(activeMember.getId(), admin.getId(), BUSINESS_DATE, BUSINESS_DATE.plusDays(1));
        createSubscription(expiredBoundaryMember.getId(), admin.getId(), BUSINESS_DATE.minusDays(5), BUSINESS_DATE);
        updateStatus(expiredBoundaryMember.getId(), AccountStatus.LOCKED);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber())
                .andExpect(jsonPath("$.data.totalPages").isNumber())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(1));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("search", activeMember.getEmail().toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Boundary Active Member"))
                .andExpect(jsonPath("$.data.content[0].hasActiveSubscription").value(true));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("search", "Boundary Ended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].accountStatus").value("LOCKED"))
                .andExpect(jsonPath("$.data.content[0].hasActiveSubscription").value(false));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("role", "ROLE_MEMBER")
                        .param("status", "LOCKED")
                        .param("search", expiredBoundaryMember.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].role").value("ROLE_MEMBER"));
    }

    @Test
    @DisplayName("Member bị AUTH-002 và request thiếu token bị ACC-005 tại Admin API")
    void adminEndpoints_ShouldEnforceRbacAndAuthentication() throws Exception {
        User member = registerUser("Regular Member", uniqueEmail("member-rbac"));
        String memberToken = login(member.getEmail(), PASSWORD, 200).at("/data/accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACC-005"));
    }

    @Test
    @DisplayName("AccountStatusGuard chặn token cũ của Admin LOCKED và DISABLED")
    void adminEndpoints_ShouldGuardCurrentAdminStatus() throws Exception {
        User lockedAdmin = registerUser("Locked Admin", uniqueEmail("locked-admin"));
        assignAdminRole(lockedAdmin);
        String lockedToken = login(lockedAdmin.getEmail(), PASSWORD, 200).at("/data/accessToken").asText();
        updateStatus(lockedAdmin.getId(), AccountStatus.LOCKED);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(lockedToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACC-004"));

        User disabledAdmin = registerUser("Disabled Admin", uniqueEmail("disabled-admin"));
        assignAdminRole(disabledAdmin);
        String disabledToken = login(disabledAdmin.getEmail(), PASSWORD, 200).at("/data/accessToken").asText();
        updateStatus(disabledAdmin.getId(), AccountStatus.DISABLED);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(disabledToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACC-006"));
    }

    @Test
    @DisplayName("Lock/unlock Member bảo toàn subscription và chặn token/login khi LOCKED")
    void lockAndUnlock_ShouldPreserveSubscriptionAndEnforceStatusImmediately() throws Exception {
        User admin = registerUser("Admin Operator", uniqueEmail("admin-operator"));
        assignAdminRole(admin);
        String adminToken = login(admin.getEmail(), PASSWORD, 200).at("/data/accessToken").asText();

        User member = registerUser("Member To Lock", uniqueEmail("member-lock"));
        String memberToken = login(member.getEmail(), PASSWORD, 200).at("/data/accessToken").asText();
        Long subscriptionId = createSubscription(
                member.getId(),
                admin.getId(),
                BUSINESS_DATE.minusDays(2),
                BUSINESS_DATE.plusDays(20)
        );
        List<?> subscriptionBefore = subscriptionSnapshot(subscriptionId);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/lock", member.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "Vi phạm nội quy phòng tập nhiều lần."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("LOCKED"))
                .andExpect(jsonPath("$.data.lockedBy").value(admin.getEmail()))
                .andExpect(jsonPath("$.data.lockedAt").value(FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE (không thay đổi)"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACC-004"));

        JsonNode lockedLogin = login(member.getEmail(), PASSWORD, 403);
        assertThat(lockedLogin.path("errorCode").asText()).isEqualTo("ACC-004");

        mockMvc.perform(patch("/api/v1/admin/users/{id}/lock", member.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "Tiếp tục vi phạm nội quy phòng tập."
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/unlock", member.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.unlockedBy").value(admin.getEmail()))
                .andExpect(jsonPath("$.data.unlockedAt").value(FIXED_INSTANT.toString()));

        assertThat(subscriptionSnapshot(subscriptionId)).isEqualTo(subscriptionBefore);
        assertThat(login(member.getEmail(), PASSWORD, 200).at("/data/accessToken").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Lock từ chối self-lock, Admin khác, DISABLED và lý do hết hạn gói")
    void lockUser_WithForbiddenTargetsOrReason_ShouldReturnVal001() throws Exception {
        User admin = registerUser("Admin One", uniqueEmail("admin-one"));
        assignAdminRole(admin);
        String adminToken = login(admin.getEmail(), PASSWORD, 200).at("/data/accessToken").asText();

        User otherAdmin = registerUser("Admin Two", uniqueEmail("admin-two"));
        assignAdminRole(otherAdmin);
        User disabledMember = registerUser("Disabled Member", uniqueEmail("disabled-member"));
        updateStatus(disabledMember.getId(), AccountStatus.DISABLED);
        User activeMember = registerUser("Active Member", uniqueEmail("active-member"));

        assertLockRejected(adminToken, admin.getId(), "Vi phạm nội quy phòng tập nghiêm trọng.");
        assertLockRejected(adminToken, otherAdmin.getId(), "Vi phạm nội quy phòng tập nghiêm trọng.");
        assertLockRejected(adminToken, disabledMember.getId(), "Vi phạm nội quy phòng tập nghiêm trọng.");
        assertLockRejected(adminToken, activeMember.getId(), "Khóa vì gói tập đã hết hạn");
        assertLockRejected(adminToken, activeMember.getId(), "short");
        assertLockRejected(adminToken, activeMember.getId(), "a".repeat(501));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/unlock", activeMember.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/unlock", disabledMember.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));
    }

    private User registerUser(String fullName, String email) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", fullName);
        payload.put("email", email);
        payload.put("password", PASSWORD);
        payload.put("confirmPassword", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated());
        entityManager.flush();
        entityManager.clear();
        return userRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    private void assignAdminRole(User user) {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        userRoleRepository.saveAndFlush(new UserRole(user, adminRole));
        entityManager.clear();
    }

    private JsonNode login(String email, String password, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void updateStatus(Long userId, AccountStatus status) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setAccountStatus(status);
        userRepository.saveAndFlush(user);
        entityManager.clear();
    }

    private Long createSubscription(
            Long memberId,
            Long adminId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String packageName = "Integration Package " + UUID.randomUUID();
        String normalizedName = packageName.toLowerCase();
        entityManager.createNativeQuery("""
                INSERT INTO membership_packages (
                    name, normalized_name, description, duration_days, price, is_active,
                    created_at, updated_at
                ) VALUES (:name, :normalizedName, 'Integration test package', 30, :price, 1,
                          CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """)
                .setParameter("name", packageName)
                .setParameter("normalizedName", normalizedName)
                .setParameter("price", new BigDecimal("500000.00"))
                .executeUpdate();
        Long packageId = ((Number) entityManager.createNativeQuery(
                        "SELECT id FROM membership_packages WHERE normalized_name = :normalizedName"
                )
                .setParameter("normalizedName", normalizedName)
                .getSingleResult()).longValue();

        entityManager.createNativeQuery("""
                INSERT INTO member_subscriptions (
                    member_id, package_id, package_name_snapshot,
                    package_duration_days_snapshot, package_price_snapshot,
                    status, start_date, end_date, approved_by_user_id, approved_at,
                    version, created_at, updated_at
                ) VALUES (
                    :memberId, :packageId, :packageName, 30, :price,
                    'ACTIVE', :startDate, :endDate, :adminId, :approvedAt,
                    0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """)
                .setParameter("memberId", memberId)
                .setParameter("packageId", packageId)
                .setParameter("packageName", packageName)
                .setParameter("price", new BigDecimal("500000.00"))
                .setParameter("startDate", Date.valueOf(startDate))
                .setParameter("endDate", Date.valueOf(endDate))
                .setParameter("adminId", adminId)
                .setParameter("approvedAt", Timestamp.from(FIXED_INSTANT))
                .executeUpdate();
        entityManager.flush();

        return ((Number) entityManager.createNativeQuery("""
                        SELECT id FROM member_subscriptions
                        WHERE member_id = :memberId AND status = 'ACTIVE'
                        """)
                .setParameter("memberId", memberId)
                .getSingleResult()).longValue();
    }

    private List<?> subscriptionSnapshot(Long subscriptionId) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT status, start_date, end_date, approved_by_user_id, approved_at, updated_at
                        FROM member_subscriptions
                        WHERE id = :id
                        """)
                .setParameter("id", subscriptionId)
                .getSingleResult();
        return List.of(row);
    }

    private void assertLockRejected(String adminToken, Long targetId, String reason) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/lock", targetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", reason))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@smartgym.test";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration
    static class FixedBusinessClockConfiguration {

        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }
    }
}
