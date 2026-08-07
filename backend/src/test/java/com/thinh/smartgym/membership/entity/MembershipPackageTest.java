package com.thinh.smartgym.membership.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipPackageTest {

    @Test
    @DisplayName("Package moi mac dinh active va luu dung du lieu schema V3")
    void constructor_ShouldCreateActivePackage() {
        MembershipPackage membershipPackage = new MembershipPackage(
                "Goi Co Ban",
                "goi co ban",
                "Tap trong 30 ngay",
                (short) 30,
                new BigDecimal("299000.00")
        );

        assertThat(membershipPackage.isActive()).isTrue();
        assertThat(membershipPackage.getName()).isEqualTo("Goi Co Ban");
        assertThat(membershipPackage.getDurationDays()).isEqualTo((short) 30);
        assertThat(membershipPackage.getPrice()).isEqualByComparingTo("299000.00");
    }

    @Test
    @DisplayName("Deactivate la soft inactive va khong xoa thong tin package")
    void deactivate_ShouldKeepPackageData() {
        MembershipPackage membershipPackage = new MembershipPackage(
                "Goi Co Ban", "goi co ban", null, (short) 30, BigDecimal.ZERO
        );

        membershipPackage.deactivate();

        assertThat(membershipPackage.isActive()).isFalse();
        assertThat(membershipPackage.getName()).isEqualTo("Goi Co Ban");
    }
}
