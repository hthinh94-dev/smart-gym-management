package com.thinh.smartgym.security;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountStatusGuardTest {

    private static final String EMAIL = "member@smartgym.com";

    @Mock
    private UserRepository userRepository;

    private AccountStatusGuard accountStatusGuard;

    @BeforeEach
    void setUp() {
        accountStatusGuard = new AccountStatusGuard(userRepository);
    }

    @Test
    @DisplayName("Cho phep tai khoan ACTIVE khi kiem tra bang email da chuan hoa")
    void validateAccountStatusByEmail_WithActiveAccount_ShouldPass() {
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.ACTIVE)));

        assertThatCode(() -> accountStatusGuard.validateAccountStatusByEmail("  MEMBER@SMARTGYM.COM  "))
                .doesNotThrowAnyException();

        verify(userRepository).findByEmailIgnoreCase(EMAIL);
    }

    @Test
    @DisplayName("Chan tai khoan LOCKED bang ma loi ACC-004")
    void validateAccountStatusByEmail_WithLockedAccount_ShouldDenyAccess() {
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.LOCKED)));

        assertThatThrownBy(() -> accountStatusGuard.validateAccountStatusByEmail(EMAIL))
                .isInstanceOfSatisfying(AccountStatusAccessDeniedException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("ACC-004");
                    assertThat(exception.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
                });
    }

    @Test
    @DisplayName("Chan tai khoan DISABLED bang ma loi ACC-006")
    void validateAccountStatusByUserId_WithDisabledAccount_ShouldDenyAccess() {
        long userId = 42L;
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.DISABLED)));

        assertThatThrownBy(() -> accountStatusGuard.validateAccountStatusByUserId(userId))
                .isInstanceOfSatisfying(AccountStatusAccessDeniedException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("ACC-006");
                    assertThat(exception.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
                });
    }

    @Test
    @DisplayName("Method Security giữ nguyên mã lỗi khi tài khoản không ACTIVE")
    void isAccountActive_WithLockedAccount_ShouldDenyAccessWithStructuredError() {
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.LOCKED)));

        assertThatThrownBy(() -> accountStatusGuard.isAccountActive(EMAIL))
                .isInstanceOfSatisfying(AccountStatusAccessDeniedException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo("ACC-004"));
    }

    /** Kiểm tra email null được chuẩn hóa và user không tồn tại bị từ chối rõ ràng. */
    @Test
    @DisplayName("Guard tu choi email null hoac khong ton tai")
    void validateAccountStatusByEmail_WithUnknownUser_ShouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmailIgnoreCase("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountStatusGuard.validateAccountStatusByEmail(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with email: ");

        verify(userRepository).findByEmailIgnoreCase("");
    }

    /** Kiểm tra user id không tồn tại không được xem như tài khoản ACTIVE. */
    @Test
    @DisplayName("Guard tu choi user id khong ton tai")
    void validateAccountStatusByUserId_WithUnknownUser_ShouldThrowUsernameNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountStatusGuard.validateAccountStatusByUserId(999L))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with id: 999");

        verify(userRepository).findById(999L);
    }

    /** Kiểm tra boolean guard trả true duy nhất cho account ACTIVE. */
    @Test
    @DisplayName("isAccountActive tra true cho tai khoan ACTIVE")
    void isAccountActive_WithActiveAccount_ShouldReturnTrue() {
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(userWithStatus(AccountStatus.ACTIVE)));

        assertThat(accountStatusGuard.isAccountActive(EMAIL)).isTrue();
    }

    private User userWithStatus(AccountStatus status) {
        return new User("Gym Member", EMAIL, "password-hash", status);
    }
}
