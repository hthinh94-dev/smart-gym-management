package com.thinh.smartgym.membership.service;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.membership.dto.MembershipPackageUpsertRequest;
import com.thinh.smartgym.membership.entity.MembershipPackage;
import com.thinh.smartgym.membership.repository.MembershipPackageRepository;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipPackageServiceTest {

    @Mock
    private MembershipPackageRepository membershipPackageRepository;

    @Mock
    private AccountStatusGuard accountStatusGuard;

    private MembershipPackageService membershipPackageService;
    private AuthenticatedUserPrincipal admin;

    @BeforeEach
    void setUp() {
        membershipPackageService = new MembershipPackageService(
                membershipPackageRepository,
                accountStatusGuard
        );
        admin = principal(RoleName.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Public list chi map contract cong khai, khong lo active va audit")
    void getPublicPackages_ShouldReturnPublicContract() {
        MembershipPackage entity = packageEntity(1L, "Goi 30 ngay", "goi 30 ngay", true);
        when(membershipPackageRepository.findByActiveTrueOrderByDurationDaysAscIdAsc())
                .thenReturn(List.of(entity));

        var response = membershipPackageService.getPublicPackages();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().isActive()).isNull();
        assertThat(response.getFirst().createdAt()).isNull();
        assertThat(response.getFirst().name()).isEqualTo("Goi 30 ngay");
    }

    @Test
    @DisplayName("Public list rong la response hop le")
    void getPublicPackages_WhenEmpty_ShouldReturnEmptyList() {
        when(membershipPackageRepository.findByActiveTrueOrderByDurationDaysAscIdAsc())
                .thenReturn(List.of());

        assertThat(membershipPackageService.getPublicPackages()).isEmpty();
    }

    @Test
    @DisplayName("Admin list tra ca active va inactive kem audit contract")
    void getAdminPackages_ShouldIncludeInactivePackages() {
        MembershipPackage active = packageEntity(1L, "Active", "active", true);
        MembershipPackage inactive = packageEntity(2L, "Inactive", "inactive", false);
        when(membershipPackageRepository.findAllByOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(inactive, active));

        var response = membershipPackageService.getAdminPackages(admin);

        assertThat(response).extracting(item -> item.isActive())
                .containsExactly(false, true);
        verify(accountStatusGuard).validateAccountStatusByUserId(101L);
    }

    @Test
    @DisplayName("Create trim va gom whitespace, lowercase normalized name")
    void createPackage_ShouldNormalizeNameAndDefaultActive() {
        when(membershipPackageRepository.existsByNormalizedName("gói cơ bản 1 tháng")).thenReturn(false);
        when(membershipPackageRepository.saveAndFlush(any(MembershipPackage.class)))
                .thenAnswer(invocation -> {
                    MembershipPackage entity = invocation.getArgument(0);
                    entity.setId(10L);
                    entity.setCreatedAt(Instant.parse("2026-08-07T08:00:00Z"));
                    entity.setUpdatedAt(Instant.parse("2026-08-07T08:00:00Z"));
                    return entity;
                });

        var response = membershipPackageService.createPackage(
                admin,
                request("  Gói   Cơ Bản  1 Tháng  ", 30, "299000.00")
        );

        assertThat(response.name()).isEqualTo("Gói Cơ Bản 1 Tháng");
        assertThat(response.isActive()).isTrue();
        verify(membershipPackageRepository).existsByNormalizedName("gói cơ bản 1 tháng");
        verify(accountStatusGuard).validateAccountStatusByUserId(101L);
    }

    @Test
    @DisplayName("Normalized name trung tra SUB-007 truoc khi ghi database")
    void createPackage_WithDuplicateNormalizedName_ShouldReject() {
        when(membershipPackageRepository.existsByNormalizedName("gói cơ bản")).thenReturn(true);

        assertErrorCode(
                () -> membershipPackageService.createPackage(
                        admin,
                        request(" GÓI   CƠ BẢN ", 30, "100000.00")
                ),
                ErrorCode.MEMBERSHIP_PACKAGE_NAME_ALREADY_EXISTS
        );
        verify(membershipPackageRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Unique race khi create duoc map SUB-007 va transaction nhan RuntimeException de rollback")
    void createPackage_WhenUniqueConstraintRaces_ShouldMapConflict() {
        when(membershipPackageRepository.existsByNormalizedName("goi race")).thenReturn(false);
        when(membershipPackageRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertErrorCode(
                () -> membershipPackageService.createPackage(admin, request("Goi Race", 30, "1.00")),
                ErrorCode.MEMBERSHIP_PACKAGE_NAME_ALREADY_EXISTS
        );
    }

    @Test
    @DisplayName("Update giu createdAt va thay du lieu hop le")
    void updatePackage_ShouldPreserveCreatedAt() {
        Instant createdAt = Instant.parse("2026-08-01T00:00:00Z");
        MembershipPackage entity = packageEntity(9L, "Old", "old", true);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        when(membershipPackageRepository.findById(9L)).thenReturn(Optional.of(entity));
        when(membershipPackageRepository.existsByNormalizedNameAndIdNot("new package", 9L)).thenReturn(false);
        when(membershipPackageRepository.saveAndFlush(entity)).thenReturn(entity);

        var response = membershipPackageService.updatePackage(
                admin, 9L, request("New Package", 90, "500000.00")
        );

        assertThat(response.name()).isEqualTo("New Package");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Unique race khi update duoc map SUB-007 de transaction rollback")
    void updatePackage_WhenUniqueConstraintRaces_ShouldMapConflict() {
        MembershipPackage entity = packageEntity(9L, "Old", "old", true);
        when(membershipPackageRepository.findById(9L)).thenReturn(Optional.of(entity));
        when(membershipPackageRepository.existsByNormalizedNameAndIdNot("new", 9L)).thenReturn(false);
        when(membershipPackageRepository.saveAndFlush(entity))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertErrorCode(
                () -> membershipPackageService.updatePackage(admin, 9L, request("New", 90, "1.00")),
                ErrorCode.MEMBERSHIP_PACKAGE_NAME_ALREADY_EXISTS
        );
    }

    @Test
    @DisplayName("Deactivate la soft inactive va van save record")
    void deactivatePackage_ShouldSetInactiveWithoutDelete() {
        MembershipPackage entity = packageEntity(5L, "Package", "package", true);
        when(membershipPackageRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(membershipPackageRepository.saveAndFlush(entity)).thenReturn(entity);

        var response = membershipPackageService.deactivatePackage(admin, 5L);

        assertThat(response.isActive()).isFalse();
        verify(membershipPackageRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Package khong ton tai tra SUB-002")
    void updatePackage_WhenNotFound_ShouldReturnSub002() {
        when(membershipPackageRepository.findById(999L)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> membershipPackageService.updatePackage(admin, 999L, request("Package", 30, "1.00")),
                ErrorCode.MEMBERSHIP_PACKAGE_NOT_FOUND
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3651})
    @DisplayName("Duration ngoai 1-3650 tra VAL-001")
    void createPackage_WithInvalidDuration_ShouldReject(int durationDays) {
        assertErrorCode(
                () -> membershipPackageService.createPackage(
                        admin, request("Package", durationDays, "100.00")
                ),
                ErrorCode.VALIDATION_ERROR
        );
        verify(membershipPackageRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Price am tra VAL-001")
    void createPackage_WithNegativePrice_ShouldReject() {
        assertErrorCode(
                () -> membershipPackageService.createPackage(
                        admin, request("Package", 30, "-0.01")
                ),
                ErrorCode.VALIDATION_ERROR
        );
    }

    private MembershipPackageUpsertRequest request(String name, int durationDays, String price) {
        return new MembershipPackageUpsertRequest(
                name,
                durationDays,
                new BigDecimal(price),
                "Mo ta package"
        );
    }

    private MembershipPackage packageEntity(Long id, String name, String normalizedName, boolean active) {
        MembershipPackage entity = new MembershipPackage(
                name, normalizedName, null, (short) 30, new BigDecimal("100000.00")
        );
        entity.setId(id);
        if (!active) {
            entity.deactivate();
        }
        return entity;
    }

    private AuthenticatedUserPrincipal principal(RoleName roleName) {
        User user = new User("Admin", "admin-package@smartgym.test", "hash", AccountStatus.ACTIVE);
        user.setId(101L);
        Role role = new Role(roleName);
        role.setId(1L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }

    private void assertErrorCode(Runnable invocation, ErrorCode expected) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expected)
                );
    }
}
