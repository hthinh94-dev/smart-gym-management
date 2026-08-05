package com.thinh.smartgym.progress.repository;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.RoleRepository;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.progress.entity.BodyProgress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BodyProgressRepositoryTest {

    private static final LocalDate RECORD_DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private BodyProgressRepository bodyProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    @DisplayName("Native upsert cùng Member/ngày giữ một dòng và cập nhật cân nặng")
    void upsertAtomic_WithSameMemberAndDate_ShouldUpdateInPlace() {
        User member = createMember();

        bodyProgressRepository.upsertAtomic(member.getId(), RECORD_DATE, new BigDecimal("72.20"));
        BodyProgress first = bodyProgressRepository
                .findByMember_IdAndRecordDate(member.getId(), RECORD_DATE)
                .orElseThrow();

        bodyProgressRepository.upsertAtomic(member.getId(), RECORD_DATE, new BigDecimal("71.80"));
        BodyProgress second = bodyProgressRepository
                .findByMember_IdAndRecordDate(member.getId(), RECORD_DATE)
                .orElseThrow();

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getWeightKg()).isEqualByComparingTo("71.80");
        assertThat(bodyProgressRepository.findByMember_IdOrderByRecordDateAsc(member.getId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("Hai ngày khác nhau tạo hai dòng lịch sử")
    void upsertAtomic_WithDifferentDates_ShouldCreateTwoRows() {
        User member = createMember();

        bodyProgressRepository.upsertAtomic(member.getId(), RECORD_DATE.minusDays(1), new BigDecimal("72.20"));
        bodyProgressRepository.upsertAtomic(member.getId(), RECORD_DATE, new BigDecimal("71.80"));

        assertThat(bodyProgressRepository.findByMember_IdOrderByRecordDateAsc(member.getId()))
                .extracting(BodyProgress::getRecordDate)
                .containsExactly(RECORD_DATE.minusDays(1), RECORD_DATE);
    }

    private User createMember() {
        Role role = roleRepository.findByName(RoleName.ROLE_MEMBER).orElseThrow();
        User member = new User(
                "Repository Member",
                "progress-repository-" + UUID.randomUUID() + "@smartgym.test",
                "hash",
                AccountStatus.ACTIVE
        );
        User saved = userRepository.saveAndFlush(member);
        userRoleRepository.saveAndFlush(new UserRole(saved, role));
        return saved;
    }
}
