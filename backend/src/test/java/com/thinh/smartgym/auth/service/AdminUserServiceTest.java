package com.thinh.smartgym.auth.service;

import com.thinh.smartgym.auth.dto.admin.LockUserRequest;
import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.auth.repository.projection.AdminUserProjection;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-30T08:15:30Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private AccountStatusGuard accountStatusGuard;

    private AdminUserService adminUserService;
    private AuthenticatedUserPrincipal admin;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userRepository,
                userRoleRepository,
                accountStatusGuard,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
        );
        admin = principal(1L, "admin@smartgym.com", RoleName.ROLE_ADMIN, AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Danh sách chuẩn hóa filter và ánh xạ PageResponse không N+1")
    void getUsers_WithFilters_ShouldReturnPageResponse() {
        AdminUserProjection projection = projection(true);
        PageRequest pageable = PageRequest.of(1, 20);
        when(userRepository.findAdminUsers(
                "ROLE_MEMBER",
                "LOCKED",
                "member",
                LocalDate.of(2026, 7, 30),
                pageable
        )).thenReturn(new PageImpl<>(List.of(projection), pageable, 45));

        var response = adminUserService.getUsers(
                admin,
                1,
                20,
                RoleName.ROLE_MEMBER,
                AccountStatus.LOCKED,
                "  MEMBER  "
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().role()).isEqualTo(RoleName.ROLE_MEMBER);
        assertThat(response.content().getFirst().hasActiveSubscription()).isTrue();
        assertThat(response.totalElements()).isEqualTo(45);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(20);
        verify(accountStatusGuard).validateAccountStatusByUserId(1L);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    @DisplayName("Size ngoài khoảng 1-100 trả VAL-001")
    void getUsers_WithInvalidSize_ShouldReject(int size) {
        assertValidationError(() -> adminUserService.getUsers(admin, 0, size, null, null, null));
        verify(userRepository, never()).findAdminUsers(any(), any(), any(), any(), any());
    }

    /** Kiểm tra page âm bị chặn trước khi tạo PageRequest hoặc gọi repository. */
    @Test
    @DisplayName("Page am tra VAL-001")
    void getUsers_WithNegativePage_ShouldReject() {
        assertValidationError(() -> adminUserService.getUsers(admin, -1, 20, null, null, null));

        verify(userRepository, never()).findAdminUsers(any(), any(), any(), any(), any());
    }

    /** Kiểm tra search trống được chuẩn hóa thành null để query không lọc sai. */
    @Test
    @DisplayName("Search trong duoc chuan hoa thanh null")
    void getUsers_WithBlankSearch_ShouldQueryWithoutSearchFilter() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.findAdminUsers(null, null, null, LocalDate.of(2026, 7, 30), pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var response = adminUserService.getUsers(admin, 0, 20, null, null, "   ");

        assertThat(response.content()).isEmpty();
        verify(userRepository).findAdminUsers(null, null, null, LocalDate.of(2026, 7, 30), pageable);
    }

    /** Kiểm tra principal admin null hoặc thiếu id bị xem là lỗi cấu hình, không truy cập database. */
    @Test
    @DisplayName("Admin principal khong hop le tra SYS-001")
    void getUsers_WithInvalidAdminPrincipal_ShouldReject() {
        assertSystemError(() -> adminUserService.getUsers(null, 0, 20, null, null, null));
        assertSystemError(() -> adminUserService.getUsers(
                principal(null, "admin@smartgym.com", RoleName.ROLE_ADMIN, AccountStatus.ACTIVE),
                0,
                20,
                null,
                null,
                null
        ));

        verify(userRepository, never()).findAdminUsers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Khóa Member ACTIVE dùng clock cố định và giữ nguyên trạng thái subscription")
    void lockUser_WithActiveMember_ShouldReturnAuditResponse() {
        User target = target(2L, AccountStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
        when(userRoleRepository.existsByUserIdAndRoleName(2L, RoleName.ROLE_ADMIN)).thenReturn(false);
        when(userRepository.countActiveSubscriptions(2L, LocalDate.of(2026, 7, 30))).thenReturn(1L);

        var response = adminUserService.lockUser(
                admin,
                2L,
                new LockUserRequest("  Vi phạm nội quy phòng tập nghiêm trọng.  ")
        );

        assertThat(target.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(response.lockedBy()).isEqualTo("admin@smartgym.com");
        assertThat(response.lockedAt()).isEqualTo(FIXED_INSTANT);
        assertThat(response.reason()).isEqualTo("Vi phạm nội quy phòng tập nghiêm trọng.");
        assertThat(response.subscriptionStatus()).isEqualTo("ACTIVE (không thay đổi)");
        verify(userRepository).saveAndFlush(target);
    }

    @Test
    @DisplayName("Không khóa lại tài khoản LOCKED")
    void lockUser_WhenAlreadyLocked_ShouldReject() {
        prepareTarget(target(2L, AccountStatus.LOCKED), false);

        assertValidationError(() -> adminUserService.lockUser(
                admin,
                2L,
                new LockUserRequest("Vi phạm nội quy phòng tập nghiêm trọng.")
        ));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Không khóa tài khoản DISABLED")
    void lockUser_WhenDisabled_ShouldReject() {
        prepareTarget(target(2L, AccountStatus.DISABLED), false);

        assertValidationError(() -> adminUserService.lockUser(
                admin,
                2L,
                new LockUserRequest("Vi phạm nội quy phòng tập nghiêm trọng.")
        ));
    }

    @Test
    @DisplayName("Admin không thể tự khóa")
    void lockUser_WhenTargetIsCurrentAdmin_ShouldReject() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(target(1L, AccountStatus.ACTIVE)));

        assertValidationError(() -> adminUserService.lockUser(
                admin,
                1L,
                new LockUserRequest("Vi phạm nội quy phòng tập nghiêm trọng.")
        ));
        verify(userRoleRepository, never()).existsByUserIdAndRoleName(any(), any());
    }

    @Test
    @DisplayName("Admin không thể khóa Admin khác trong MVP")
    void lockUser_WhenTargetHasAdminRole_ShouldReject() {
        prepareTarget(target(3L, AccountStatus.ACTIVE), true);

        assertValidationError(() -> adminUserService.lockUser(
                admin,
                3L,
                new LockUserRequest("Vi phạm nội quy phòng tập nghiêm trọng.")
        ));
    }

    @Test
    @DisplayName("Lý do khóa dưới 10 ký tự bị từ chối")
    void lockUser_WithShortReason_ShouldReject() {
        assertValidationError(() -> adminUserService.lockUser(admin, 2L, new LockUserRequest("Quá ngắn")));
        verify(userRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Lý do khóa trên 500 ký tự bị từ chối")
    void lockUser_WithLongReason_ShouldReject() {
        assertValidationError(() -> adminUserService.lockUser(admin, 2L, new LockUserRequest("a".repeat(501))));
    }

    /** Kiểm tra request/reason null bị chặn bằng validation error thay vì NullPointerException. */
    @Test
    @DisplayName("Lock request null tra VAL-001")
    void lockUser_WithNullRequest_ShouldReject() {
        assertValidationError(() -> adminUserService.lockUser(admin, 2L, null));

        verify(userRepository, never()).findByIdForUpdate(any());
    }

    /** Kiểm tra target id null hoặc không dương bị chặn trước câu query khóa row. */
    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("Target id khong hop le tra VAL-001")
    void lockUser_WithInvalidTargetId_ShouldReject(Long targetUserId) {
        assertValidationError(() -> adminUserService.lockUser(
                admin,
                targetUserId,
                new LockUserRequest("Vi phạm nội quy phòng tập nghiêm trọng.")
        ));

        verify(userRepository, never()).findByIdForUpdate(any());
    }

    /** Kiểm tra target hợp lệ về id nhưng không tồn tại trả lỗi nghiệp vụ ổn định. */
    @Test
    @DisplayName("Target khong ton tai tra VAL-001")
    void lockUser_WithUnknownTarget_ShouldReject() {
        when(userRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertValidationError(() -> adminUserService.lockUser(
                admin,
                999L,
                new LockUserRequest("Vi phạm nội quy phòng tập nghiêm trọng.")
        ));

        verify(userRoleRepository, never()).existsByUserIdAndRoleName(any(), any());
    }

    /** Kiểm tra khóa account không có subscription vẫn không thay đổi dữ liệu gói tập. */
    @Test
    @DisplayName("Lock user khong co subscription tra NO_ACTIVE_SUBSCRIPTION")
    void lockUser_WithoutActiveSubscription_ShouldPreserveSubscriptionState() {
        User target = target(2L, AccountStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
        when(userRoleRepository.existsByUserIdAndRoleName(2L, RoleName.ROLE_ADMIN)).thenReturn(false);
        when(userRepository.countActiveSubscriptions(2L, LocalDate.of(2026, 7, 30))).thenReturn(0L);

        var response = adminUserService.lockUser(
                admin,
                2L,
                new LockUserRequest("Vi phạm nội quy phòng tập nghiêm trọng.")
        );

        assertThat(response.subscriptionStatus()).isEqualTo("NO_ACTIVE_SUBSCRIPTION (không thay đổi)");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Khóa vì gói tập đã hết hạn",
            "Membership expired nên khóa tài khoản",
            "Subscription expiration requires account lock"
    })
    @DisplayName("Không dùng hết hạn subscription làm lý do khóa")
    void lockUser_WithSubscriptionExpirationReason_ShouldReject(String reason) {
        assertValidationError(() -> adminUserService.lockUser(admin, 2L, new LockUserRequest(reason)));
    }

    @Test
    @DisplayName("Mở khóa chỉ chuyển LOCKED thành ACTIVE với clock cố định")
    void unlockUser_WhenLocked_ShouldActivateAccount() {
        User target = target(2L, AccountStatus.LOCKED);
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));

        var response = adminUserService.unlockUser(admin, 2L);

        assertThat(target.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.unlockedBy()).isEqualTo("admin@smartgym.com");
        assertThat(response.unlockedAt()).isEqualTo(FIXED_INSTANT);
        verify(userRepository).saveAndFlush(target);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "DISABLED"})
    @DisplayName("Mở khóa từ trạng thái khác LOCKED bị từ chối")
    void unlockUser_WhenNotLocked_ShouldReject(String status) {
        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(target(2L, AccountStatus.valueOf(status))));

        assertValidationError(() -> adminUserService.unlockUser(admin, 2L));
        verify(userRepository, never()).saveAndFlush(any());
    }

    private void prepareTarget(User target, boolean hasAdminRole) {
        when(userRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        when(userRoleRepository.existsByUserIdAndRoleName(target.getId(), RoleName.ROLE_ADMIN))
                .thenReturn(hasAdminRole);
    }

    private User target(Long id, AccountStatus status) {
        User user = new User("Target Member", "target@smartgym.com", "password-hash", status);
        user.setId(id);
        user.setCreatedAt(Instant.parse("2026-07-20T08:00:00Z"));
        return user;
    }

    private AuthenticatedUserPrincipal principal(
            Long id,
            String email,
            RoleName roleName,
            AccountStatus status
    ) {
        User user = new User("Admin Local", email, "password-hash", status);
        user.setId(id);
        user.setCreatedAt(Instant.parse("2026-07-20T08:00:00Z"));
        Role role = new Role(roleName);
        role.setId(roleName == RoleName.ROLE_ADMIN ? 1L : 2L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }

    private AdminUserProjection projection(boolean activeSubscription) {
        return new AdminUserProjection() {
            public Long getId() { return 2L; }
            public String getFullName() { return "Target Member"; }
            public String getEmail() { return "target@smartgym.com"; }
            public String getRole() { return "ROLE_MEMBER"; }
            public String getAccountStatus() { return "LOCKED"; }
            public Timestamp getCreatedAt() {
                return Timestamp.from(Instant.parse("2026-07-20T08:00:00Z"));
            }
            public Integer getHasActiveSubscription() { return activeSubscription ? 1 : 0; }
        };
    }

    private void assertValidationError(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    private void assertSystemError(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_CONFIGURATION_ERROR));
    }
}
