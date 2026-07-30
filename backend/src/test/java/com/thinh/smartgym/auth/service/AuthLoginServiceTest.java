package com.thinh.smartgym.auth.service;

import com.thinh.smartgym.auth.dto.LoginRequest;
import com.thinh.smartgym.auth.dto.LoginResponse;
import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.RoleRepository;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import com.thinh.smartgym.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthLoginServiceTest {

    private static final String EMAIL = "member@smartgym.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                passwordEncoder,
                authenticationManager,
                jwtService
        );
    }

    @Test
    @DisplayName("Login thành công chuẩn hóa email, giữ nguyên password và cấp JWT")
    void login_WithValidCredentials_ShouldReturnConfiguredTokenResponse() {
        LoginRequest request = new LoginRequest("  MEMBER@SMARTGYM.COM  ", " SecretPass1 ");
        User user = user(AccountStatus.ACTIVE);
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(userRepository.findByEmailWithRolesIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(principal)).thenReturn("signed-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getValue().getPrincipal()).isEqualTo(EMAIL);
        assertThat(authenticationCaptor.getValue().getCredentials()).isEqualTo(" SecretPass1 ");
        assertThat(response.accessToken()).isEqualTo("signed-access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user().id()).isEqualTo(101L);
        assertThat(response.user().role()).isEqualTo(RoleName.ROLE_MEMBER);
    }

    @Test
    @DisplayName("Email không tồn tại trả ACC-007 và không cấp token")
    void login_WithUnknownEmail_ShouldReturnAcc007WithoutToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertLoginError(new LoginRequest("missing@smartgym.com", "AnyPassword"), ErrorCode.INVALID_CREDENTIALS);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Sai password trả ACC-007 và không cấp token")
    void login_WithWrongPassword_ShouldReturnAcc007WithoutToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertLoginError(new LoginRequest(EMAIL, "WrongPassword"), ErrorCode.INVALID_CREDENTIALS);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Tài khoản LOCKED trả ACC-004 và không cấp token")
    void login_WithLockedAccount_ShouldReturnAcc004WithoutToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new LockedException("locked"));

        assertLoginError(new LoginRequest(EMAIL, "SecurePass1"), ErrorCode.ACCOUNT_LOCKED);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Tài khoản DISABLED trả ACC-006 và không cấp token")
    void login_WithDisabledAccount_ShouldReturnAcc006WithoutToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new DisabledException("disabled"));

        assertLoginError(new LoginRequest(EMAIL, "SecurePass1"), ErrorCode.ACCOUNT_DISABLED);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Trạng thái đổi sau authenticate vẫn chặn trước khi cấp token")
    void login_WhenAccountBecomesLockedAfterAuthentication_ShouldNotIssueToken() {
        User activeUser = user(AccountStatus.ACTIVE);
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(activeUser);
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(userRepository.findByEmailWithRolesIgnoreCase(EMAIL)).thenReturn(Optional.of(user(AccountStatus.LOCKED)));

        assertLoginError(new LoginRequest(EMAIL, "SecurePass1"), ErrorCode.ACCOUNT_LOCKED);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Loi ha tang xac thuc tra SYS-001 thay vi gia mao sai thong tin dang nhap")
    void login_WhenAuthenticationInfrastructureFails_ShouldReturnSys001WithoutToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new AuthenticationServiceException("authentication backend unavailable"));

        assertLoginError(new LoginRequest(EMAIL, "SecurePass1"), ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        verify(jwtService, never()).generateAccessToken(any());
    }

    private void assertLoginError(LoginRequest request, ErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private User user(AccountStatus accountStatus) {
        User user = new User("Gym Member", EMAIL, "$2a$12$password-hash", accountStatus);
        user.setId(101L);
        user.setCreatedAt(Instant.parse("2026-07-29T08:00:00Z"));
        Role role = new Role(RoleName.ROLE_MEMBER);
        role.setId(2L);
        user.attachUserRole(new UserRole(user, role));
        return user;
    }
}
