package com.thinh.smartgym.auth.controller;

import com.thinh.smartgym.auth.dto.admin.AdminUserResponse;
import com.thinh.smartgym.auth.dto.admin.LockUserRequest;
import com.thinh.smartgym.auth.dto.admin.LockUserResponse;
import com.thinh.smartgym.auth.dto.admin.UnlockUserResponse;
import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.service.AdminUserService;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.GlobalExceptionHandler;
import com.thinh.smartgym.common.response.PageResponse;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import com.thinh.smartgym.security.CustomUserDetailsService;
import com.thinh.smartgym.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminUserController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("GET admin/users trả PageResponse đúng contract")
    void getUsers_WithAdminPrincipal_ShouldReturnPage() throws Exception {
        AuthenticatedUserPrincipal principal = adminPrincipal();
        AdminUserResponse user = new AdminUserResponse(
                2L,
                "Gym Member",
                "member@smartgym.com",
                RoleName.ROLE_MEMBER,
                AccountStatus.ACTIVE,
                Instant.parse("2026-07-30T08:00:00Z"),
                true
        );
        when(adminUserService.getUsers(principal, 0, 20, null, null, null))
                .thenReturn(new PageResponse<>(List.of(user), 1, 1, 0, 20));

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(authentication(authenticationToken(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("member@smartgym.com"))
                .andExpect(jsonPath("$.data.content[0].hasActiveSubscription").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("GET admin/users chuyển đúng filter vào Service")
    void getUsers_WithFilters_ShouldDelegateFilters() throws Exception {
        AuthenticatedUserPrincipal principal = adminPrincipal();
        when(adminUserService.getUsers(
                principal,
                2,
                10,
                RoleName.ROLE_MEMBER,
                AccountStatus.LOCKED,
                "member"
        )).thenReturn(new PageResponse<>(List.of(), 0, 0, 2, 10));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "2")
                        .param("size", "10")
                        .param("role", "ROLE_MEMBER")
                        .param("status", "LOCKED")
                        .param("search", "member")
                        .with(authentication(authenticationToken(principal))))
                .andExpect(status().isOk());

        verify(adminUserService).getUsers(
                principal,
                2,
                10,
                RoleName.ROLE_MEMBER,
                AccountStatus.LOCKED,
                "member"
        );
    }

    @Test
    @DisplayName("Size lớn hơn 100 trả VAL-001")
    void getUsers_WithOversizedPage_ShouldReturnVal001() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("size", "101")
                        .with(authentication(authenticationToken(adminPrincipal()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));
    }

    @Test
    @DisplayName("PATCH lock trim reason và trả audit response")
    void lockUser_WithValidRequest_ShouldReturnResponse() throws Exception {
        AuthenticatedUserPrincipal principal = adminPrincipal();
        when(adminUserService.lockUser(eq(principal), eq(2L), any(LockUserRequest.class)))
                .thenReturn(new LockUserResponse(
                        2L,
                        "Gym Member",
                        AccountStatus.LOCKED,
                        "admin@smartgym.com",
                        Instant.parse("2026-07-30T08:15:30Z"),
                        "Vi phạm nội quy phòng tập nghiêm trọng.",
                        "ACTIVE (không thay đổi)"
                ));

        mockMvc.perform(patch("/api/v1/admin/users/2/lock")
                        .with(csrf())
                        .with(authentication(authenticationToken(principal)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  Vi phạm nội quy phòng tập nghiêm trọng.  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.accountStatus").value("LOCKED"))
                .andExpect(jsonPath("$.data.lockedBy").value("admin@smartgym.com"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE (không thay đổi)"));
    }

    @Test
    @DisplayName("PATCH lock reason dưới 10 ký tự trả VAL-001")
    void lockUser_WithShortReason_ShouldReturnVal001() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/2/lock")
                        .with(csrf())
                        .with(authentication(authenticationToken(adminPrincipal())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"))
                .andExpect(jsonPath("$.details.field").value("reason"));
    }

    @Test
    @DisplayName("PATCH unlock trả đúng actor và timestamp")
    void unlockUser_WhenLocked_ShouldReturnResponse() throws Exception {
        AuthenticatedUserPrincipal principal = adminPrincipal();
        when(adminUserService.unlockUser(principal, 2L)).thenReturn(new UnlockUserResponse(
                2L,
                "Gym Member",
                AccountStatus.ACTIVE,
                "admin@smartgym.com",
                Instant.parse("2026-07-30T09:00:00Z")
        ));

        mockMvc.perform(patch("/api/v1/admin/users/2/unlock")
                        .with(csrf())
                        .with(authentication(authenticationToken(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.unlockedBy").value("admin@smartgym.com"))
                .andExpect(jsonPath("$.data.unlockedAt").value("2026-07-30T09:00:00Z"));
    }

    private UsernamePasswordAuthenticationToken authenticationToken(AuthenticatedUserPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AuthenticatedUserPrincipal adminPrincipal() {
        User user = new User("Admin Local", "admin@smartgym.com", "password-hash", AccountStatus.ACTIVE);
        user.setId(1L);
        user.setCreatedAt(Instant.parse("2026-07-29T08:00:00Z"));
        Role role = new Role(RoleName.ROLE_ADMIN);
        role.setId(1L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }
}
