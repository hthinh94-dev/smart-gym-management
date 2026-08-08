package com.thinh.smartgym.membership.subscription.entity;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.common.persistence.BaseEntity;
import com.thinh.smartgym.membership.entity.MembershipPackage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "member_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private MembershipPackage membershipPackage;

    @Column(name = "package_name_snapshot", nullable = false, length = 100)
    private String packageNameSnapshot;

    @Column(name = "package_duration_days_snapshot", nullable = false)
    private Short packageDurationDaysSnapshot;

    @Column(name = "package_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal packagePriceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public MemberSubscription(User member, MembershipPackage membershipPackage) {
        this.member = Objects.requireNonNull(member, "member is required");
        this.membershipPackage = Objects.requireNonNull(membershipPackage, "membershipPackage is required");
        this.packageNameSnapshot = membershipPackage.getName();
        this.packageDurationDaysSnapshot = membershipPackage.getDurationDays();
        this.packagePriceSnapshot = membershipPackage.getPrice();
        this.status = SubscriptionStatus.PENDING;
    }

    @Override
    public String toString() {
        return "MemberSubscription{id=" + id + ", status=" + status + "}";
    }
}
