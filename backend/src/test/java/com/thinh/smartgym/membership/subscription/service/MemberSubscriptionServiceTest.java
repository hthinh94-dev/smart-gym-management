package com.thinh.smartgym.membership.subscription.service;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.membership.entity.MembershipPackage;
import com.thinh.smartgym.membership.repository.MembershipPackageRepository;
import com.thinh.smartgym.membership.subscription.dto.CreateSubscriptionRequest;
import com.thinh.smartgym.membership.subscription.entity.MemberSubscription;
import com.thinh.smartgym.membership.subscription.entity.SubscriptionStatus;
import com.thinh.smartgym.membership.subscription.repository.MemberSubscriptionRepository;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberSubscriptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T04:00:00Z");

    @Mock
    private MemberSubscriptionRepository memberSubscriptionRepository;
    @Mock
    private MembershipPackageRepository membershipPackageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountStatusGuard accountStatusGuard;

    private MemberSubscriptionService service;
    private User member;
    private AuthenticatedUserPrincipal principal;
    private MembershipPackage membershipPackage;

    @BeforeEach
    void setUp() {
        service = new MemberSubscriptionService(
                memberSubscriptionRepository,
                membershipPackageRepository,
                userRepository,
                accountStatusGuard,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        member = user(101L, RoleName.ROLE_MEMBER);
        principal = AuthenticatedUserPrincipal.from(member);
        membershipPackage = membershipPackage(2L, true);
    }

    @Test
    @DisplayName("Tao request PENDING thanh cong voi snapshot va khong co ngay hieu luc")
    void createNewSubscription_ShouldReturnPendingSnapshot() {
        prepareCreationChecks();
        when(memberSubscriptionRepository.saveAndFlush(any(MemberSubscription.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), 55L));

        var response = service.createNewSubscription(principal, new CreateSubscriptionRequest(2L));

        assertThat(response.subscriptionId()).isEqualTo(55L);
        assertThat(response.memberId()).isEqualTo(101L);
        assertThat(response.packageId()).isEqualTo(2L);
        assertThat(response.packageName()).isEqualTo("Gói 90 ngày");
        assertThat(response.price()).isEqualByComparingTo("1200000.00");
        assertThat(response.status()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(response.requestedAt()).isEqualTo(NOW);
        assertThat(response.startDate()).isNull();
        assertThat(response.endDate()).isNull();
        assertThat(response.approvedAt()).isNull();
        verify(accountStatusGuard).validateAccountStatusByUserId(101L);
        verify(userRepository).findByIdForUpdate(101L);
    }

    @Test
    @DisplayName("Package khong ton tai tra SUB-002")
    void createNewSubscription_WhenPackageMissing_ShouldReturnSub002() {
        when(userRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(member));
        when(membershipPackageRepository.findById(999L)).thenReturn(Optional.empty());

        assertError(() -> service.createNewSubscription(
                principal, new CreateSubscriptionRequest(999L)), ErrorCode.MEMBERSHIP_PACKAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("Package inactive tra SUB-003")
    void createNewSubscription_WhenPackageInactive_ShouldReturnSub003() {
        MembershipPackage inactive = membershipPackage(2L, false);
        when(userRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(member));
        when(membershipPackageRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertError(() -> service.createNewSubscription(
                principal, new CreateSubscriptionRequest(2L)), ErrorCode.MEMBERSHIP_PACKAGE_INACTIVE);
        verify(memberSubscriptionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Subscription ACTIVE hop le tra SUB-004")
    void createNewSubscription_WhenCurrentActiveExists_ShouldReturnSub004() {
        prepareBase();
        MemberSubscription active = activeSubscription();
        when(memberSubscriptionRepository.findCurrentByMemberId(101L, LocalDate.of(2026, 8, 8)))
                .thenReturn(Optional.of(active));

        assertError(() -> service.createNewSubscription(
                principal, new CreateSubscriptionRequest(2L)), ErrorCode.ACTIVE_SUBSCRIPTION_ALREADY_EXISTS);
        verify(memberSubscriptionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Request PENDING da ton tai tra SUB-006")
    void createNewSubscription_WhenPendingExists_ShouldReturnSub006() {
        prepareBase();
        when(memberSubscriptionRepository.findCurrentByMemberId(101L, LocalDate.of(2026, 8, 8)))
                .thenReturn(Optional.empty());
        MemberSubscription pending = persisted(new MemberSubscription(member, membershipPackage), 54L);
        when(memberSubscriptionRepository.findByMemberIdAndStatus(101L, SubscriptionStatus.PENDING))
                .thenReturn(Optional.of(pending));

        assertError(() -> service.createNewSubscription(
                principal, new CreateSubscriptionRequest(2L)), ErrorCode.PENDING_SUBSCRIPTION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Unique constraint race duoc map thanh SUB-006")
    void createNewSubscription_WhenUniqueConstraintRaces_ShouldReturnSub006() {
        prepareCreationChecks();
        when(memberSubscriptionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_member_subscriptions_one_pending"));

        assertError(() -> service.createNewSubscription(
                principal, new CreateSubscriptionRequest(2L)), ErrorCode.PENDING_SUBSCRIPTION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("GET current tra snapshot va daysRemaining theo endDate exclusive")
    void getCurrentSubscription_ShouldReturnOwnedCurrentSubscription() {
        MemberSubscription active = activeSubscription();
        when(memberSubscriptionRepository.findCurrentByMemberId(101L, LocalDate.of(2026, 8, 8)))
                .thenReturn(Optional.of(active));

        var response = service.getCurrentSubscription(principal);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.daysRemaining()).isEqualTo(24L);
        assertThat(response.packageName()).isEqualTo("Gói 90 ngày");
    }

    @Test
    @DisplayName("GET current khong thay tra SUB-005")
    void getCurrentSubscription_WhenMissing_ShouldReturnSub005() {
        when(memberSubscriptionRepository.findCurrentByMemberId(101L, LocalDate.of(2026, 8, 8)))
                .thenReturn(Optional.empty());

        assertError(() -> service.getCurrentSubscription(principal), ErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    @Test
    @DisplayName("Admin khong duoc goi Member Subscription Service")
    void createNewSubscription_WhenPrincipalIsAdmin_ShouldDeny() {
        AuthenticatedUserPrincipal admin = AuthenticatedUserPrincipal.from(user(202L, RoleName.ROLE_ADMIN));

        assertThatThrownBy(() -> service.createNewSubscription(admin, new CreateSubscriptionRequest(2L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(accountStatusGuard, never()).validateAccountStatusByUserId(any());
    }

    private void prepareBase() {
        when(userRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(member));
        when(membershipPackageRepository.findById(2L)).thenReturn(Optional.of(membershipPackage));
    }

    private void prepareCreationChecks() {
        prepareBase();
        when(memberSubscriptionRepository.findCurrentByMemberId(101L, LocalDate.of(2026, 8, 8)))
                .thenReturn(Optional.empty());
        when(memberSubscriptionRepository.findByMemberIdAndStatus(101L, SubscriptionStatus.PENDING))
                .thenReturn(Optional.empty());
    }

    private MemberSubscription activeSubscription() {
        MemberSubscription subscription = persisted(new MemberSubscription(member, membershipPackage), 48L);
        ReflectionTestUtils.setField(subscription, "status", SubscriptionStatus.ACTIVE);
        ReflectionTestUtils.setField(subscription, "startDate", LocalDate.of(2026, 8, 1));
        ReflectionTestUtils.setField(subscription, "endDate", LocalDate.of(2026, 9, 1));
        ReflectionTestUtils.setField(subscription, "approvedAt", Instant.parse("2026-08-01T02:00:00Z"));
        return subscription;
    }

    private MemberSubscription persisted(MemberSubscription subscription, Long id) {
        ReflectionTestUtils.setField(subscription, "id", id);
        subscription.setCreatedAt(NOW);
        subscription.setUpdatedAt(NOW);
        return subscription;
    }

    private MembershipPackage membershipPackage(Long id, boolean active) {
        MembershipPackage entity = new MembershipPackage(
                "Gói 90 ngày",
                "gói 90 ngày",
                "Mô tả",
                (short) 90,
                new BigDecimal("1200000.00")
        );
        entity.setId(id);
        if (!active) {
            entity.deactivate();
        }
        return entity;
    }

    private User user(Long id, RoleName roleName) {
        User user = new User("User", "user" + id + "@smartgym.test", "hash", AccountStatus.ACTIVE);
        user.setId(id);
        Role role = new Role(roleName);
        role.setId(roleName == RoleName.ROLE_ADMIN ? 1L : 2L);
        user.attachUserRole(new UserRole(user, role));
        return user;
    }

    private void assertError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
