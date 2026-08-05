package com.thinh.smartgym.progress.entity;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.common.enums.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BodyProgressTest {

    @Test
    @DisplayName("BodyProgress ánh xạ member, ngày và cân nặng")
    void constructor_ShouldSetBusinessFields() {
        User member = new User("Member", "entity-progress@test.com", "hash", AccountStatus.ACTIVE);
        BodyProgress progress = new BodyProgress(
                member,
                LocalDate.of(2026, 8, 5),
                new BigDecimal("72.20")
        );

        assertThat(progress.getMember()).isSameAs(member);
        assertThat(progress.getRecordDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(progress.getWeightKg()).isEqualByComparingTo("72.20");
        assertThat(progress.toString()).contains("recordDate=2026-08-05");
    }
}
