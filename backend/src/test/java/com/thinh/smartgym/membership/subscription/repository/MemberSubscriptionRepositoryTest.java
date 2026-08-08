package com.thinh.smartgym.membership.subscription.repository;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.membership.entity.MembershipPackage;
import com.thinh.smartgym.membership.repository.MembershipPackageRepository;
import com.thinh.smartgym.membership.subscription.entity.MemberSubscription;
import com.thinh.smartgym.membership.subscription.entity.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MemberSubscriptionRepositoryTest {

    @Autowired
    private MemberSubscriptionRepository memberSubscriptionRepository;

    @Autowired
    private MembershipPackageRepository membershipPackageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    @DisplayName("Current query dung bien endDate exclusive va ownership member")
    void currentQuery_ShouldUseExclusiveEndDateAndMemberOwnership() {
        User owner = saveMember("owner");
        User other = saveMember("other");
        MembershipPackage membershipPackage = savePackage();
        MemberSubscription active = saveActive(owner, membershipPackage,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));

        assertThat(memberSubscriptionRepository.findCurrentByMemberId(
                owner.getId(), LocalDate.of(2026, 8, 31)))
                .contains(active);
        assertThat(memberSubscriptionRepository.findCurrentByMemberId(
                owner.getId(), LocalDate.of(2026, 9, 1)))
                .isEmpty();
        assertThat(memberSubscriptionRepository.findCurrentByMemberId(
                other.getId(), LocalDate.of(2026, 8, 31)))
                .isEmpty();
        assertThat(memberSubscriptionRepository.findByIdAndMemberId(active.getId(), other.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Hai transaction dong thoi chi tao duoc mot PENDING")
    void uniqueConstraint_ShouldRejectConcurrentPendingRequests() throws Exception {
        TransactionTemplate transaction = requiresNewTransaction();
        Long[] ids = new Long[2];
        transaction.executeWithoutResult(status -> {
            ids[0] = saveMember("race").getId();
            ids[1] = savePackage().getId();
        });

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var task = (java.util.concurrent.Callable<Void>) () -> {
                start.await(5, TimeUnit.SECONDS);
                try {
                    transaction.executeWithoutResult(status -> {
                        User member = userRepository.findById(ids[0]).orElseThrow();
                        MembershipPackage membershipPackage = membershipPackageRepository
                                .findById(ids[1]).orElseThrow();
                        memberSubscriptionRepository.saveAndFlush(
                                new MemberSubscription(member, membershipPackage)
                        );
                    });
                    successes.incrementAndGet();
                } catch (DataIntegrityViolationException exception) {
                    conflicts.incrementAndGet();
                }
                return null;
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);

            assertThat(successes).hasValue(1);
            assertThat(conflicts).hasValue(1);
            assertThat(memberSubscriptionRepository.findByMemberIdAndStatus(
                    ids[0], SubscriptionStatus.PENDING)).isPresent();
        } finally {
            executor.shutdownNow();
            transaction.executeWithoutResult(status -> {
                memberSubscriptionRepository.findByMemberIdAndStatus(ids[0], SubscriptionStatus.PENDING)
                        .ifPresent(memberSubscriptionRepository::delete);
                memberSubscriptionRepository.flush();
                membershipPackageRepository.deleteById(ids[1]);
                userRepository.deleteById(ids[0]);
            });
        }
    }

    private TransactionTemplate requiresNewTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private User saveMember(String prefix) {
        User user = new User(
                "Subscription Member",
                prefix + "." + UUID.randomUUID() + "@smartgym.test",
                "password-hash",
                AccountStatus.ACTIVE
        );
        return userRepository.saveAndFlush(user);
    }

    private MembershipPackage savePackage() {
        String suffix = UUID.randomUUID().toString();
        return membershipPackageRepository.saveAndFlush(new MembershipPackage(
                "Package " + suffix,
                "package " + suffix,
                null,
                (short) 30,
                new BigDecimal("500000.00")
        ));
    }

    private MemberSubscription saveActive(
            User member,
            MembershipPackage membershipPackage,
            LocalDate startDate,
            LocalDate endDate
    ) {
        MemberSubscription subscription = new MemberSubscription(member, membershipPackage);
        ReflectionTestUtils.setField(subscription, "status", SubscriptionStatus.ACTIVE);
        ReflectionTestUtils.setField(subscription, "startDate", startDate);
        ReflectionTestUtils.setField(subscription, "endDate", endDate);
        ReflectionTestUtils.setField(subscription, "approvedAt", Instant.parse("2026-08-01T00:00:00Z"));
        return memberSubscriptionRepository.saveAndFlush(subscription);
    }
}
