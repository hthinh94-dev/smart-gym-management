package com.thinh.smartgym.progress.controller;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.progress.dto.BodyProgressResponse;
import com.thinh.smartgym.progress.service.BodyProgressService;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BodyProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BodyProgressService bodyProgressService;

    @Test
    @DisplayName("POST Body Progress trả response an toàn")
    void post_ShouldReturnBodyProgressResponse() throws Exception {
        AuthenticatedUserPrincipal principal = principal(RoleName.ROLE_MEMBER);
        when(bodyProgressService.upsertCurrentProgress(any(), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/member/body-progress")
                        .with(authentication(authenticationToken(principal)))
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(305))
                .andExpect(jsonPath("$.data.memberId").value(101))
                .andExpect(jsonPath("$.data.recordDate").value("2026-08-05"))
                .andExpect(jsonPath("$.data.weightKg").value(72.2))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("GET Body Progress trả danh sách rỗng đúng contract")
    void getHistory_WhenEmpty_ShouldReturnEmptyList() throws Exception {
        when(bodyProgressService.getCurrentProgress(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/member/body-progress")
                        .with(authentication(authenticationToken(principal(RoleName.ROLE_MEMBER)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Validation weightKg bằng 0 trả VAL-001 và không gọi Service")
    void post_WithZeroWeight_ShouldReturnVal001() throws Exception {
        mockMvc.perform(post("/api/v1/member/body-progress")
                        .with(authentication(authenticationToken(principal(RoleName.ROLE_MEMBER))))
                        .contentType("application/json")
                        .content(validRequest().replace("72.20", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));

        verify(bodyProgressService, never()).upsertCurrentProgress(any(), any());
    }

    @Test
    @DisplayName("Guest nhận ACC-005 và Admin nhận AUTH-002")
    void endpoints_ShouldEnforceAuthenticationAndMemberRole() throws Exception {
        mockMvc.perform(get("/api/v1/member/body-progress"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACC-005"));

        mockMvc.perform(get("/api/v1/member/body-progress")
                        .with(authentication(authenticationToken(principal(RoleName.ROLE_ADMIN)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));
    }

    @Test
    @DisplayName("OpenAPI công bố hai operation Body Progress có bearerAuth")
    void openApi_ShouldExposeBodyProgressContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/member/body-progress'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/member/body-progress'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/member/body-progress'].post.responses['400']").exists());
    }

    private BodyProgressResponse response() {
        Instant created = Instant.parse("2026-08-05T00:00:00Z");
        return new BodyProgressResponse(
                305L,
                101L,
                LocalDate.of(2026, 8, 5),
                new BigDecimal("72.20"),
                created,
                created
        );
    }

    private String validRequest() {
        return """
                {
                  "recordDate": "2026-08-05",
                  "weightKg": 72.20
                }
                """;
    }

    private UsernamePasswordAuthenticationToken authenticationToken(AuthenticatedUserPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AuthenticatedUserPrincipal principal(RoleName roleName) {
        User user = new User("Progress Member", "progress-controller@smartgym.test", "hash", AccountStatus.ACTIVE);
        user.setId(101L);
        Role role = new Role(roleName);
        role.setId(roleName == RoleName.ROLE_ADMIN ? 1L : 2L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }
}
