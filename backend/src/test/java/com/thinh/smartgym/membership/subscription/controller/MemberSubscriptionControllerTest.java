package com.thinh.smartgym.membership.subscription.controller;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.membership.subscription.dto.CreateSubscriptionRequest;
import com.thinh.smartgym.membership.subscription.dto.SubscriptionResponse;
import com.thinh.smartgym.membership.subscription.entity.SubscriptionStatus;
import com.thinh.smartgym.membership.subscription.service.MemberSubscriptionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class MemberSubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberSubscriptionService memberSubscriptionService;

    @Test
    @DisplayName("Member tao request PENDING nhan HTTP 201 va contract an du lieu noi bo")
    void createSubscription_AsMember_ShouldReturnCreatedContract() throws Exception {
        AuthenticatedUserPrincipal member = principal(RoleName.ROLE_MEMBER);
        when(memberSubscriptionService.createNewSubscription(eq(member), any(CreateSubscriptionRequest.class)))
                .thenReturn(pendingResponse());

        mockMvc.perform(post("/api/v1/member/subscriptions")
                        .with(authentication(authenticationToken(member)))
                        .contentType("application/json")
                        .content("{\"packageId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subscriptionId").value(55))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.startDate").doesNotExist())
                .andExpect(jsonPath("$.data.endDate").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.member").doesNotExist())
                .andExpect(jsonPath("$.data.membershipPackage").doesNotExist());
    }

    @Test
    @DisplayName("packageId khong hop le tra VAL-001")
    void createSubscription_WithInvalidPackageId_ShouldReturnValidationError() throws Exception {
        AuthenticatedUserPrincipal member = principal(RoleName.ROLE_MEMBER);

        mockMvc.perform(post("/api/v1/member/subscriptions")
                        .with(authentication(authenticationToken(member)))
                        .contentType("application/json")
                        .content("{\"packageId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-001"));
        verify(memberSubscriptionService, never()).createNewSubscription(any(), any());
    }

    @Test
    @DisplayName("Guest tao subscription nhan ACC-005")
    void createSubscription_AsGuest_ShouldReturnAcc005() throws Exception {
        mockMvc.perform(post("/api/v1/member/subscriptions")
                        .contentType("application/json")
                        .content("{\"packageId\":2}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACC-005"));
    }

    @Test
    @DisplayName("Admin goi Member API nhan AUTH-002")
    void createSubscription_AsAdmin_ShouldReturnAuth002() throws Exception {
        AuthenticatedUserPrincipal admin = principal(RoleName.ROLE_ADMIN);

        mockMvc.perform(post("/api/v1/member/subscriptions")
                        .with(authentication(authenticationToken(admin)))
                        .contentType("application/json")
                        .content("{\"packageId\":2}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"));
    }

    @Test
    @DisplayName("GET current tra ACTIVE va daysRemaining")
    void getCurrent_AsMember_ShouldReturnCurrentSubscription() throws Exception {
        AuthenticatedUserPrincipal member = principal(RoleName.ROLE_MEMBER);
        when(memberSubscriptionService.getCurrentSubscription(member)).thenReturn(activeResponse());

        mockMvc.perform(get("/api/v1/member/subscriptions/current")
                        .with(authentication(authenticationToken(member))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.startDate").value("2026-08-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.daysRemaining").value(24));
    }

    @Test
    @DisplayName("OpenAPI khai bao bearer va day du response Ngay 17")
    void openApi_ShouldDocumentSubscriptionEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions/current'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/member/subscriptions/current'].get.responses['404']").exists());
    }

    private UsernamePasswordAuthenticationToken authenticationToken(AuthenticatedUserPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AuthenticatedUserPrincipal principal(RoleName roleName) {
        User user = new User("User", roleName.name().toLowerCase() + "@smartgym.test", "hash", AccountStatus.ACTIVE);
        user.setId(roleName == RoleName.ROLE_MEMBER ? 101L : 202L);
        Role role = new Role(roleName);
        role.setId(roleName == RoleName.ROLE_ADMIN ? 1L : 2L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }

    private SubscriptionResponse pendingResponse() {
        return new SubscriptionResponse(
                55L, 101L, 2L, "Gói 90 ngày", new BigDecimal("1200000.00"),
                SubscriptionStatus.PENDING, Instant.parse("2026-08-08T04:00:00Z"),
                null, null, null, null
        );
    }

    private SubscriptionResponse activeResponse() {
        return new SubscriptionResponse(
                48L, 101L, 2L, "Gói 90 ngày", new BigDecimal("1200000.00"),
                SubscriptionStatus.ACTIVE, Instant.parse("2026-08-01T02:00:00Z"),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), 24L,
                Instant.parse("2026-08-01T02:00:00Z")
        );
    }
}
