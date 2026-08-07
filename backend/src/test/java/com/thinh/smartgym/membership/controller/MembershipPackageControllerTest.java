package com.thinh.smartgym.membership.controller;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.membership.dto.MembershipPackageResponse;
import com.thinh.smartgym.membership.service.MembershipPackageService;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MembershipPackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipPackageService membershipPackageService;

    @Test
    @DisplayName("Public GET khong can token va khong lo field noi bo")
    void publicGet_ShouldAllowGuestAndReturnPublicContract() throws Exception {
        when(membershipPackageService.getPublicPackages()).thenReturn(List.of(publicResponse()));

        mockMvc.perform(get("/api/v1/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Goi 30 ngay"))
                .andExpect(jsonPath("$.data[0].isActive").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data[0].normalizedName").doesNotExist());
    }

    @Test
    @DisplayName("Public GET danh sach rong van tra 200")
    void publicGet_WhenEmpty_ShouldReturnEmptyArray() throws Exception {
        when(membershipPackageService.getPublicPackages()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Admin tao package tra 201 va response quan tri")
    void createPackage_AsAdmin_ShouldReturnCreated() throws Exception {
        when(membershipPackageService.createPackage(any(), any())).thenReturn(adminResponse(true));

        mockMvc.perform(post("/api/v1/admin/packages")
                        .with(authentication(authenticationToken(RoleName.ROLE_ADMIN)))
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("Admin update va soft inactive package thanh cong")
    void updateAndDeactivate_AsAdmin_ShouldReturnSuccess() throws Exception {
        when(membershipPackageService.updatePackage(any(), eq(1L), any()))
                .thenReturn(adminResponse(true));
        when(membershipPackageService.deactivatePackage(any(), eq(1L)))
                .thenReturn(adminResponse(false));

        mockMvc.perform(put("/api/v1/admin/packages/1")
                        .with(authentication(authenticationToken(RoleName.ROLE_ADMIN)))
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));

        mockMvc.perform(delete("/api/v1/admin/packages/1")
                        .with(authentication(authenticationToken(RoleName.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    @DisplayName("Member nhan AUTH-002 va Guest nhan ACC-005 tren Admin API")
    void adminEndpoints_ShouldEnforceAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/packages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACC-005"));

        mockMvc.perform(get("/api/v1/admin/packages")
                        .with(authentication(authenticationToken(RoleName.ROLE_MEMBER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));
    }

    @Test
    @DisplayName("Duration va price khong hop le tra VAL-001 truoc Service")
    void createPackage_WithInvalidInput_ShouldReturnVal001() throws Exception {
        mockMvc.perform(post("/api/v1/admin/packages")
                        .with(authentication(authenticationToken(RoleName.ROLE_ADMIN)))
                        .contentType("application/json")
                        .content(validRequest().replace("30", "0").replace("299000.00", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));

        verify(membershipPackageService, never()).createPackage(any(), any());
    }

    @Test
    @DisplayName("Name rong va description qua dai tra VAL-001")
    void createPackage_WithInvalidTextFields_ShouldReturnVal001() throws Exception {
        String invalidRequest = """
                {
                  "name": "  ",
                  "durationDays": 30,
                  "price": 1000.00,
                  "description": "%s"
                }
                """.formatted("x".repeat(1001));

        mockMvc.perform(post("/api/v1/admin/packages")
                        .with(authentication(authenticationToken(RoleName.ROLE_ADMIN)))
                        .contentType("application/json")
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));

        verify(membershipPackageService, never()).createPackage(any(), any());
    }

    @Test
    @DisplayName("Package khong ton tai tra SUB-002")
    void updatePackage_WhenNotFound_ShouldReturnSub002() throws Exception {
        when(membershipPackageService.updatePackage(any(), eq(999L), any()))
                .thenThrow(new BusinessException(
                        ErrorCode.MEMBERSHIP_PACKAGE_NOT_FOUND,
                        Map.of("packageId", 999L)
                ));

        mockMvc.perform(put("/api/v1/admin/packages/999")
                        .with(authentication(authenticationToken(RoleName.ROLE_ADMIN)))
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SUB-002"));
    }

    @Test
    @DisplayName("OpenAPI cong bo public va bon operation Admin voi bearerAuth")
    void openApi_ShouldExposePackageContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/packages'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/admin/packages'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/admin/packages'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/admin/packages/{id}'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/admin/packages/{id}'].delete.security[0].bearerAuth").isArray());
    }

    private MembershipPackageResponse publicResponse() {
        return new MembershipPackageResponse(
                1L, "Goi 30 ngay", 30, new BigDecimal("299000.00"), "Mo ta", null, null, null
        );
    }

    private MembershipPackageResponse adminResponse(boolean active) {
        Instant now = Instant.parse("2026-08-07T08:00:00Z");
        return new MembershipPackageResponse(
                1L, "Goi 30 ngay", 30, new BigDecimal("299000.00"), "Mo ta", active, now, now
        );
    }

    private String validRequest() {
        return """
                {
                  "name": "Goi 30 ngay",
                  "durationDays": 30,
                  "price": 299000.00,
                  "description": "Mo ta goi tap"
                }
                """;
    }

    private UsernamePasswordAuthenticationToken authenticationToken(RoleName roleName) {
        AuthenticatedUserPrincipal principal = principal(roleName);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AuthenticatedUserPrincipal principal(RoleName roleName) {
        User user = new User("Package User", "package-controller@smartgym.test", "hash", AccountStatus.ACTIVE);
        user.setId(101L);
        Role role = new Role(roleName);
        role.setId(roleName == RoleName.ROLE_ADMIN ? 1L : 2L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }
}
