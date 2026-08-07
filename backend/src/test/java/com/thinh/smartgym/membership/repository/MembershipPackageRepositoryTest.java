package com.thinh.smartgym.membership.repository;

import com.thinh.smartgym.membership.entity.MembershipPackage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MembershipPackageRepositoryTest {

    @Autowired
    private MembershipPackageRepository membershipPackageRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    @DisplayName("Public query chi tra active va sap xep duration, id on dinh")
    void publicQuery_ShouldReturnOnlyActivePackagesInStableOrder() {
        String suffix = UUID.randomUUID().toString();
        MembershipPackage longPackage = save("Long " + suffix, "long " + suffix, 90, true);
        MembershipPackage inactivePackage = save("Inactive " + suffix, "inactive " + suffix, 15, false);
        MembershipPackage shortPackage = save("Short " + suffix, "short " + suffix, 30, true);

        var result = membershipPackageRepository.findByActiveTrueOrderByDurationDaysAscIdAsc();

        assertThat(result).contains(shortPackage, longPackage).doesNotContain(inactivePackage);
        assertThat(result.indexOf(shortPackage)).isLessThan(result.indexOf(longPackage));
    }

    @Test
    @Transactional
    @DisplayName("Soft inactive giu record trong database va Admin van doc duoc")
    void softInactive_ShouldRetainDatabaseRecord() {
        String suffix = UUID.randomUUID().toString();
        MembershipPackage membershipPackage = save("Retained " + suffix, "retained " + suffix, 30, true);

        membershipPackage.deactivate();
        membershipPackageRepository.saveAndFlush(membershipPackage);

        assertThat(membershipPackageRepository.findById(membershipPackage.getId()))
                .get()
                .extracting(MembershipPackage::isActive)
                .isEqualTo(false);
        assertThat(membershipPackageRepository.findByActiveTrueOrderByDurationDaysAscIdAsc())
                .doesNotContain(membershipPackage);
    }

    @Test
    @Transactional
    @DisplayName("Unique constraint chan hai normalized name trung nhau")
    void uniqueConstraint_ShouldRejectDuplicateNormalizedName() {
        String normalizedName = "duplicate " + UUID.randomUUID();
        save("First package", normalizedName, 30, true);

        MembershipPackage duplicate = new MembershipPackage(
                "Second package", normalizedName, null, (short) 60, BigDecimal.TEN
        );

        assertThatThrownBy(() -> membershipPackageRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Create loi unique rollback toan bo transaction tren MySQL")
    void createError_ShouldRollbackDatabaseTransaction() {
        String normalizedName = "create rollback " + UUID.randomUUID();
        TransactionTemplate transaction = requiresNewTransaction();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            save("First rollback package", normalizedName, 30, true);
            save("Duplicate rollback package", normalizedName, 60, true);
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(membershipPackageRepository.findByNormalizedName(normalizedName)).isEmpty();
    }

    @Test
    @DisplayName("Update loi unique rollback ve normalized name truoc do tren MySQL")
    void updateError_ShouldRestorePreviousDatabaseState() {
        String suffix = UUID.randomUUID().toString();
        String firstNormalized = "update target " + suffix;
        String originalNormalized = "update original " + suffix;
        TransactionTemplate transaction = requiresNewTransaction();
        Long[] ids = new Long[2];

        transaction.executeWithoutResult(status -> {
            ids[0] = save("Update target", firstNormalized, 30, true).getId();
            ids[1] = save("Update original", originalNormalized, 60, true).getId();
        });

        try {
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                MembershipPackage updating = membershipPackageRepository.findById(ids[1]).orElseThrow();
                updating.update(
                        "Conflicting update",
                        firstNormalized,
                        null,
                        (short) 90,
                        BigDecimal.TEN
                );
                membershipPackageRepository.saveAndFlush(updating);
            })).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(membershipPackageRepository.findById(ids[1]))
                    .get()
                    .extracting(MembershipPackage::getNormalizedName)
                    .isEqualTo(originalNormalized);
        } finally {
            transaction.executeWithoutResult(status -> {
                membershipPackageRepository.deleteById(ids[1]);
                membershipPackageRepository.deleteById(ids[0]);
                membershipPackageRepository.flush();
            });
        }
    }

    private TransactionTemplate requiresNewTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private MembershipPackage save(String name, String normalizedName, int durationDays, boolean active) {
        MembershipPackage membershipPackage = new MembershipPackage(
                name,
                normalizedName,
                null,
                (short) durationDays,
                new BigDecimal("100000.00")
        );
        if (!active) {
            membershipPackage.deactivate();
        }
        return membershipPackageRepository.saveAndFlush(membershipPackage);
    }
}
