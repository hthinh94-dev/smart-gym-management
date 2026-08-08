package com.thinh.smartgym.membership.subscription.repository;

import com.thinh.smartgym.membership.subscription.entity.MemberSubscription;
import com.thinh.smartgym.membership.subscription.entity.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {

    Optional<MemberSubscription> findByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

    @Query("""
            SELECT subscription
            FROM MemberSubscription subscription
            WHERE subscription.member.id = :memberId
              AND subscription.status = com.thinh.smartgym.membership.subscription.entity.SubscriptionStatus.ACTIVE
              AND subscription.startDate <= :businessDate
              AND subscription.endDate > :businessDate
            """)
    Optional<MemberSubscription> findCurrentByMemberId(
            @Param("memberId") Long memberId,
            @Param("businessDate") LocalDate businessDate
    );

    Optional<MemberSubscription> findByIdAndMemberId(Long subscriptionId, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT subscription
            FROM MemberSubscription subscription
            WHERE subscription.member.id = :memberId
            ORDER BY subscription.id
            """)
    List<MemberSubscription> findAllByMemberIdForUpdate(@Param("memberId") Long memberId);
}
