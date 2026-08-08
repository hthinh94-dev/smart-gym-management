package com.thinh.smartgym.membership.subscription.entity;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.membership.entity.MembershipPackage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MemberSubscriptionTest {

    @Test
    @DisplayName("Subscription moi snapshot package va bat dau o PENDING")
    void constructor_ShouldCreatePendingSnapshot() {
        User member = member();
        MembershipPackage membershipPackage = membershipPackage();

        MemberSubscription subscription = new MemberSubscription(member, membershipPackage);

        assertThat(subscription.getMember()).isSameAs(member);
        assertThat(subscription.getMembershipPackage()).isSameAs(membershipPackage);
        assertThat(subscription.getPackageNameSnapshot()).isEqualTo("Gói 90 ngày");
        assertThat(subscription.getPackageDurationDaysSnapshot()).isEqualTo((short) 90);
        assertThat(subscription.getPackagePriceSnapshot()).isEqualByComparingTo("1200000.00");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(subscription.getStartDate()).isNull();
        assertThat(subscription.getEndDate()).isNull();
        assertThat(subscription.getApprovedAt()).isNull();
        assertThat(subscription.getCancelledAt()).isNull();
    }

    @Test
    @DisplayName("Snapshot khong thay doi khi package bi cap nhat sau do")
    void snapshot_ShouldRemainStableWhenPackageChanges() {
        MembershipPackage membershipPackage = membershipPackage();
        MemberSubscription subscription = new MemberSubscription(member(), membershipPackage);

        membershipPackage.update(
                "Gói mới",
                "gói mới",
                null,
                (short) 180,
                new BigDecimal("2200000.00")
        );

        assertThat(subscription.getPackageNameSnapshot()).isEqualTo("Gói 90 ngày");
        assertThat(subscription.getPackageDurationDaysSnapshot()).isEqualTo((short) 90);
        assertThat(subscription.getPackagePriceSnapshot()).isEqualByComparingTo("1200000.00");
    }

    @Test
    @DisplayName("toString khong truy cap lazy graph")
    void toString_ShouldOnlyExposeLocalState() {
        MemberSubscription subscription = new MemberSubscription(member(), membershipPackage());

        assertThat(subscription.toString()).isEqualTo("MemberSubscription{id=null, status=PENDING}");
    }

    private User member() {
        return new User("Member", "member@smartgym.test", "hash", AccountStatus.ACTIVE);
    }

    private MembershipPackage membershipPackage() {
        return new MembershipPackage(
                "Gói 90 ngày",
                "gói 90 ngày",
                "Mô tả",
                (short) 90,
                new BigDecimal("1200000.00")
        );
    }
}
