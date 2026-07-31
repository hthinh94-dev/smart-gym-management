package com.thinh.smartgym.security;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserPrincipalTest {

    /** Kiểm tra principal sao chép đầy đủ identity và metadata cần cho authentication/session. */
    @Test
    @DisplayName("Principal sao chep dung thong tin User")
    void from_WithValidUser_ShouldCopyIdentityFields() {
        Instant createdAt = Instant.parse("2026-07-31T08:00:00Z");
        User user = userWithStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(createdAt);
        attachRole(user, 20L, RoleName.ROLE_MEMBER);

        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);

        assertThat(principal.getId()).isEqualTo(10L);
        assertThat(principal.getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(principal.getEmail()).isEqualTo("member@smartgym.com");
        assertThat(principal.getUsername()).isEqualTo("member@smartgym.com");
        assertThat(principal.getPassword()).isEqualTo("secret-hash");
        assertThat(principal.getCreatedAt()).isEqualTo(createdAt);
        assertThat(principal.getPrimaryRole()).isEqualTo(RoleName.ROLE_MEMBER);
    }

    /** Kiểm tra role được loại trùng, sắp xếp ổn định và role ưu tiên không phụ thuộc thứ tự Set. */
    @Test
    @DisplayName("Principal loai trung va sap xep roles theo thu tu he thong")
    void from_WithMultipleRoles_ShouldDeduplicateAndSortAuthorities() {
        User user = userWithStatus(AccountStatus.ACTIVE);
        attachRole(user, 23L, RoleName.ROLE_PT);
        attachRole(user, 22L, RoleName.ROLE_MEMBER);
        attachRole(user, 21L, RoleName.ROLE_ADMIN);
        attachRole(user, 24L, RoleName.ROLE_MEMBER);

        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);

        assertThat(principal.getPrimaryRole()).isEqualTo(RoleName.ROLE_ADMIN);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_MEMBER", "ROLE_PT");
    }

    /** Kiểm tra association role null hoặc role name null bị bỏ qua thay vì gây NullPointerException. */
    @Test
    @DisplayName("Principal bo qua association role khong day du")
    void from_WithIncompleteRoleAssociations_ShouldIgnoreThem() {
        User user = userWithStatus(AccountStatus.ACTIVE);
        user.attachUserRole(new UserRole(user, null));
        attachRole(user, 20L, null);
        attachRole(user, 21L, RoleName.ROLE_MEMBER);

        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MEMBER");
    }

    /** Kiểm tra user không có system role bị từ chối để tránh tạo authentication thiếu quyền. */
    @Test
    @DisplayName("Principal tu choi User khong co role hop le")
    void from_WithoutValidRole_ShouldThrowUsernameNotFoundException() {
        User user = userWithStatus(AccountStatus.ACTIVE);

        assertThatThrownBy(() -> AuthenticatedUserPrincipal.from(user))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User has no assigned system role");
    }

    /** Kiểm tra cờ account status khớp chính xác với contract UserDetails của Spring Security. */
    @ParameterizedTest(name = "Trang thai {0}")
    @EnumSource(AccountStatus.class)
    @DisplayName("Principal anh xa dung trang thai khoa va vo hieu hoa")
    void accountStatus_ShouldControlUserDetailsFlags(AccountStatus status) {
        User user = userWithStatus(status);
        attachRole(user, 20L, RoleName.ROLE_MEMBER);

        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);

        assertThat(principal.isAccountNonLocked()).isEqualTo(status != AccountStatus.LOCKED);
        assertThat(principal.isEnabled()).isEqualTo(status != AccountStatus.DISABLED);
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.getAccountStatus()).isEqualTo(status);
    }

    /** Kiểm tra collection authority là bất biến để caller không nâng quyền sau authentication. */
    @Test
    @DisplayName("Principal khong cho sua authorities")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void authorities_ShouldBeImmutable() {
        User user = userWithStatus(AccountStatus.ACTIVE);
        attachRole(user, 20L, RoleName.ROLE_MEMBER);
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);

        Collection authorities = principal.getAuthorities();

        assertThatThrownBy(() -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /** Kiểm tra log representation không làm lộ password hash của principal. */
    @Test
    @DisplayName("Principal toString khong lam lo password hash")
    void toString_ShouldNotExposePasswordHash() {
        User user = userWithStatus(AccountStatus.ACTIVE);
        attachRole(user, 20L, RoleName.ROLE_MEMBER);

        String representation = AuthenticatedUserPrincipal.from(user).toString();

        assertThat(representation)
                .contains("member@smartgym.com", "ACTIVE")
                .doesNotContain("secret-hash");
    }

    /** Kiểm tra exception trạng thái khóa/vô hiệu hóa giữ đúng mã lỗi nghiệp vụ. */
    @Test
    @DisplayName("AccountStatusAccessDeniedException map dung ma loi")
    void accountStatusException_ShouldMapLockedAndDisabledCodes() {
        AccountStatusAccessDeniedException locked =
                new AccountStatusAccessDeniedException(AccountStatus.LOCKED);
        AccountStatusAccessDeniedException disabled =
                new AccountStatusAccessDeniedException(AccountStatus.DISABLED);

        assertThat(locked.getErrorCode()).isEqualTo("ACC-004");
        assertThat(locked.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(locked.getMessage()).isNotBlank();
        assertThat(disabled.getErrorCode()).isEqualTo("ACC-006");
        assertThat(disabled.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(disabled.getMessage()).isNotBlank();
    }

    /** Kiểm tra ACTIVE không thể bị biểu diễn thành access-denied exception. */
    @Test
    @DisplayName("Khong cho tao access denied cho tai khoan ACTIVE")
    void accountStatusException_WithActiveStatus_ShouldThrowException() {
        assertThatThrownBy(() -> new AccountStatusAccessDeniedException(AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ACTIVE accounts must not be denied");
    }

    private User userWithStatus(AccountStatus status) {
        User user = new User("Nguyễn Văn An", "member@smartgym.com", "secret-hash", status);
        user.setId(10L);
        return user;
    }

    private void attachRole(User user, Long roleId, RoleName roleName) {
        Role role = new Role(roleName);
        role.setId(roleId);
        user.attachUserRole(new UserRole(user, role));
    }
}
