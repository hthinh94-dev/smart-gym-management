package com.thinh.smartgym.security;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final String EMAIL = "member@smartgym.com";

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("Chuẩn hóa email và cho phép tài khoản ACTIVE đăng nhập")
    void loadUserByUsername_WithActiveAccount_ShouldReturnEnabledUser() {
        when(userRepository.findByEmailWithRolesIgnoreCase(EMAIL))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.ACTIVE)));

        AuthenticatedUserPrincipal userDetails = (AuthenticatedUserPrincipal) userDetailsService
                .loadUserByUsername("  MEMBER@SMARTGYM.COM  ");

        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.getId()).isEqualTo(101L);
        assertThat(userDetails.getFullName()).isEqualTo("Gym Member");
        assertThat(userDetails.getEmail()).isEqualTo(EMAIL);
        assertThat(userDetails.getPrimaryRole()).isEqualTo(RoleName.ROLE_MEMBER);
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_MEMBER");
        verify(userRepository).findByEmailWithRolesIgnoreCase(EMAIL);
    }

    @Test
    @DisplayName("Ánh xạ tài khoản LOCKED sang UserDetails bị khóa")
    void loadUserByUsername_WithLockedAccount_ShouldReturnLockedUser() {
        when(userRepository.findByEmailWithRolesIgnoreCase(EMAIL))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.LOCKED)));

        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(userDetails.isAccountNonLocked()).isFalse();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Ánh xạ tài khoản DISABLED sang UserDetails bị vô hiệu hóa")
    void loadUserByUsername_WithDisabledAccount_ShouldReturnDisabledUser() {
        when(userRepository.findByEmailWithRolesIgnoreCase(EMAIL))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.DISABLED)));

        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    /** Kiểm tra email không tồn tại bị từ chối bằng contract chuẩn của UserDetailsService. */
    @Test
    @DisplayName("Tu choi email khong ton tai sau khi chuan hoa")
    void loadUserByUsername_WithUnknownEmail_ShouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmailWithRolesIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("  MEMBER@SMARTGYM.COM "))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with email: " + EMAIL);

        verify(userRepository).findByEmailWithRolesIgnoreCase(EMAIL);
    }

    /** Kiểm tra null email được chuẩn hóa an toàn và không gây NullPointerException. */
    @Test
    @DisplayName("Null email duoc chuan hoa thanh chuoi rong")
    void loadUserByUsername_WithNullEmail_ShouldQueryEmptyIdentity() {
        when(userRepository.findByEmailWithRolesIgnoreCase("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with email: ");

        verify(userRepository).findByEmailWithRolesIgnoreCase("");
    }

    private User userWithStatus(AccountStatus status) {
        User user = new User("Gym Member", EMAIL, "password-hash", status);
        user.setId(101L);
        user.setCreatedAt(Instant.parse("2026-07-29T08:00:00Z"));
        Role role = new Role(RoleName.ROLE_MEMBER);
        role.setId(2L);
        user.attachUserRole(new UserRole(user, role));
        return user;
    }
}
