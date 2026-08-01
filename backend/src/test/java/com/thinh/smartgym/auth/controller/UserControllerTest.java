package com.thinh.smartgym.auth.controller;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.GlobalExceptionHandler;
import com.thinh.smartgym.security.AccountStatusAccessDeniedException;
import com.thinh.smartgym.security.AccountStatusGuard;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountStatusGuard accountStatusGuard;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("GET users/me trả đúng principal và gọi AccountStatusGuard bằng user ID")
    void currentUser_WithAuthenticatedPrincipal_ShouldReturnCurrentUser() throws Exception {
        AuthenticatedUserPrincipal principal = principal(AccountStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/users/me").with(authentication(authenticationToken(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.fullName").value("Gym Member"))
                .andExpect(jsonPath("$.data.email").value("member@smartgym.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_MEMBER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-07-29T08:00:00Z"));

        verify(accountStatusGuard).validateAccountStatusByUserId(101L);
    }

    @Test
    @DisplayName("Guard chặn token cũ của tài khoản LOCKED bằng ACC-004")
    void currentUser_WhenAccountLocked_ShouldReturnAcc004() throws Exception {
        AuthenticatedUserPrincipal principal = principal(AccountStatus.ACTIVE);
        doThrow(new AccountStatusAccessDeniedException(AccountStatus.LOCKED))
                .when(accountStatusGuard).validateAccountStatusByUserId(101L);

        mockMvc.perform(get("/api/v1/users/me").with(authentication(authenticationToken(principal))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACC-004"))
                .andExpect(jsonPath("$.details.accountStatus").value("LOCKED"));
    }

    private UsernamePasswordAuthenticationToken authenticationToken(AuthenticatedUserPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private AuthenticatedUserPrincipal principal(AccountStatus status) {
        User user = new User("Gym Member", "member@smartgym.com", "password-hash", status);
        user.setId(101L);
        user.setCreatedAt(Instant.parse("2026-07-29T08:00:00Z"));
        Role role = new Role(RoleName.ROLE_MEMBER);
        role.setId(2L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }
}
