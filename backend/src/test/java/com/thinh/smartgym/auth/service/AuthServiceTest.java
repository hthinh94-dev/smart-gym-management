package com.thinh.smartgym.auth.service;

import com.thinh.smartgym.auth.dto.RegisterRequest;
import com.thinh.smartgym.auth.dto.RegisterResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-28T08:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        authService = new AuthService(userRepository, roleRepository, userRoleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Dang ky thanh cong tao tai khoan ACTIVE va ROLE_MEMBER")
    void register_WithValidRequest_ShouldCreateActiveMember() {
        Role memberRole = memberRole();
        stubSuccessfulRegistration(memberRole);

        RegisterResponse response = authService.register(validRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        verify(userRoleRepository).saveAndFlush(userRoleCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedUser.getEmail()).isEqualTo("user@gmail.com");
        assertThat(savedUser.getFullName()).isEqualTo("Nguyen Van An");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecurePass1");
        assertThat(passwordEncoder.matches("SecurePass1", savedUser.getPasswordHash())).isTrue();

        UserRole savedUserRole = userRoleCaptor.getValue();
        assertThat(savedUserRole.getUser()).isSameAs(savedUser);
        assertThat(savedUserRole.getRole().getName()).isEqualTo(RoleName.ROLE_MEMBER);
        assertThat(savedUser.getUserRoles()).containsExactly(savedUserRole);

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.fullName()).isEqualTo("Nguyen Van An");
        assertThat(response.email()).isEqualTo("user@gmail.com");
        assertThat(response.role()).isEqualTo(RoleName.ROLE_MEMBER);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("Email va ho ten duoc trim, email duoc chuyen lowercase truoc khi truy van")
    void register_ShouldNormalizeIdentityBeforeCheckingAndSaving() {
        Role memberRole = memberRole();
        stubSuccessfulRegistration(memberRole);

        authService.register(new RegisterRequest(
                "  Nguyen Van An  ",
                "  User@Gmail.Com  ",
                "SecurePass1",
                "SecurePass1"
        ));

        verify(userRepository).existsByEmailIgnoreCase("user@gmail.com");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Nguyen Van An");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@gmail.com");
    }

    @Test
    @DisplayName("Email trung bi tu choi bang ACC-001 truoc khi ghi database")
    void register_WithExistingEmail_ShouldThrowAcc001() {
        when(userRepository.existsByEmailIgnoreCase("user@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
                    assertThat(exception.getDetails().toString()).contains("user@gmail.com");
                });

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(roleRepository, userRoleRepository);
    }

    @ParameterizedTest(name = "Password policy case {index}")
    @MethodSource("invalidPasswords")
    @DisplayName("Password vi pham do dai, chu hoa, chu so hoac khoang trang bien")
    void register_WithInvalidPassword_ShouldThrowAcc002(String invalidPassword) {
        RegisterRequest request = new RegisterRequest(
                "Nguyen Van An",
                "user@gmail.com",
                invalidPassword,
                invalidPassword
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
                    assertThat(exception.getDetails().toString()).contains("password");
                });

        verifyNoInteractions(userRepository, roleRepository, userRoleRepository);
    }

    @Test
    @DisplayName("Confirm password khong khop bi tu choi bang ACC-002")
    void register_WithMismatchedConfirmation_ShouldThrowAcc002() {
        RegisterRequest request = new RegisterRequest(
                "Nguyen Van An",
                "user@gmail.com",
                "SecurePass1",
                "DifferentPass1"
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
                    assertThat(exception.getDetails().toString()).contains("confirmPassword");
                });

        verifyNoInteractions(userRepository, roleRepository, userRoleRepository);
    }

    @Test
    @DisplayName("Race condition tren uk_users_email van duoc chuyen thanh ACC-001")
    void register_WhenEmailUniqueConstraintWinsRace_ShouldThrowAcc001() {
        when(userRepository.existsByEmailIgnoreCase("user@gmail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_MEMBER)).thenReturn(Optional.of(memberRole()));
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(
                new DataIntegrityViolationException("Duplicate entry for key uk_users_email")
        );

        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(userRoleRepository, never()).saveAndFlush(any(UserRole.class));
    }

    @Test
    @DisplayName("Thieu ROLE_MEMBER la loi cau hinh noi bo va khong tao User")
    void register_WhenMemberRoleIsMissing_ShouldReturnInternalConfigurationError() {
        when(userRepository.existsByEmailIgnoreCase("user@gmail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_MEMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(validRequest()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_CONFIGURATION_ERROR));

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(userRoleRepository);
    }

    private void stubSuccessfulRegistration(Role memberRole) {
        when(userRepository.existsByEmailIgnoreCase("user@gmail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_MEMBER)).thenReturn(Optional.of(memberRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            user.setCreatedAt(CREATED_AT);
            user.setUpdatedAt(CREATED_AT);
            return user;
        });
        when(userRoleRepository.saveAndFlush(any(UserRole.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Role memberRole() {
        Role role = new Role(RoleName.ROLE_MEMBER);
        role.setId(2L);
        return role;
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "  Nguyen Van An  ",
                "  User@Gmail.Com  ",
                "SecurePass1",
                "SecurePass1"
        );
    }

    private static Stream<String> invalidPasswords() {
        return Stream.of(
                "Pass1",
                "A1" + "a".repeat(71),
                "securepass1",
                "SecurePassword",
                " SecurePass1",
                "SecurePass1 "
        );
    }
}
