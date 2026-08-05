package com.thinh.smartgym.progress;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.RoleRepository;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.progress.dto.BodyProgressResponse;
import com.thinh.smartgym.progress.dto.BodyProgressUpsertRequest;
import com.thinh.smartgym.progress.repository.BodyProgressRepository;
import com.thinh.smartgym.progress.service.BodyProgressService;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(BodyProgressIntegrationTest.FixedClockConfiguration.class)
@Transactional
class BodyProgressIntegrationTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 5);

    @Autowired
    private BodyProgressService bodyProgressService;

    @Autowired
    private BodyProgressRepository bodyProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Ghi cùng ngày update-in-place và không làm mất ngày trước")
    void upsert_ShouldKeepOneRowPerMemberAndDate() {
        User member = createMember();
        AuthenticatedUserPrincipal principal = principal(member);

        BodyProgressResponse first = bodyProgressService.upsertCurrentProgress(
                principal, request(BUSINESS_DATE.minusDays(1), "72.20")
        );
        BodyProgressResponse updated = bodyProgressService.upsertCurrentProgress(
                principal, request(BUSINESS_DATE, "71.80")
        );
        BodyProgressResponse updatedAgain = bodyProgressService.upsertCurrentProgress(
                principal, request(BUSINESS_DATE, "71.50")
        );

        assertThat(updatedAgain.id()).isEqualTo(updated.id());
        assertThat(updatedAgain.weightKg()).isEqualByComparingTo("71.50");
        assertThat(first.recordDate()).isEqualTo(BUSINESS_DATE.minusDays(1));
        assertThat(bodyProgressRepository.findByMember_IdOrderByRecordDateAsc(member.getId()))
                .hasSize(2);
    }

    @Test
    @DisplayName("Ngày tương lai bị từ chối và lịch sử vẫn rỗng")
    void upsert_WithFutureDate_ShouldRejectWithoutWriting() {
        User member = createMember();

        assertThatThrownBy(() -> bodyProgressService.upsertCurrentProgress(
                principal(member), request(BUSINESS_DATE.plusDays(1), "72.20")
        )).hasMessageContaining("không hợp lệ");

        List<?> history = bodyProgressRepository.findByMember_IdOrderByRecordDateAsc(member.getId());
        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("Đổi timezone mặc định không làm lệch ngày nghiệp vụ Việt Nam")
    void upsert_ShouldUseBusinessDateFromClock() {
        User member = createMember();
        BodyProgressResponse response = bodyProgressService.upsertCurrentProgress(
                principal(member), request(BUSINESS_DATE, "72.20")
        );

        assertThat(response.recordDate()).isEqualTo(BUSINESS_DATE);
    }

    private BodyProgressUpsertRequest request(LocalDate date, String weight) {
        return new BodyProgressUpsertRequest(date, new BigDecimal(weight));
    }

    private AuthenticatedUserPrincipal principal(User user) {
        return AuthenticatedUserPrincipal.from(userRepository
                .findByEmailWithRolesIgnoreCase(user.getEmail()).orElseThrow());
    }

    private User createMember() {
        Role role = roleRepository.findByName(RoleName.ROLE_MEMBER).orElseThrow();
        User member = new User(
                "Integration Member",
                "progress-integration-" + UUID.randomUUID() + "@smartgym.test",
                "hash",
                AccountStatus.ACTIVE
        );
        User saved = userRepository.saveAndFlush(member);
        userRoleRepository.saveAndFlush(new UserRole(saved, role));
        entityManager.clear();
        return userRepository.findByEmailWithRolesIgnoreCase(saved.getEmail()).orElseThrow();
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
