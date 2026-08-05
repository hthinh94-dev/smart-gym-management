package com.thinh.smartgym.progress.service;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.progress.dto.BodyProgressResponse;
import com.thinh.smartgym.progress.dto.BodyProgressUpsertRequest;
import com.thinh.smartgym.progress.entity.BodyProgress;
import com.thinh.smartgym.progress.repository.BodyProgressRepository;
import com.thinh.smartgym.security.AccountStatusAccessDeniedException;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BodyProgressServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

    @Mock
    private BodyProgressRepository bodyProgressRepository;

    @Mock
    private AccountStatusGuard accountStatusGuard;

    private BodyProgressService service;
    private final TimeZone originalTimeZone = TimeZone.getDefault();

    @BeforeEach
    void setUp() {
        service = new BodyProgressService(
                bodyProgressRepository,
                accountStatusGuard,
                Clock.fixed(Instant.parse("2026-08-04T18:00:00Z"), ZoneOffset.UTC)
        );
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("Upsert dùng ID principal và trả DTO an toàn")
    void upsert_ShouldUsePrincipalAndReturnResponse() {
        AuthenticatedUserPrincipal principal = principal(101L, RoleName.ROLE_MEMBER);
        BodyProgress progress = progress(101L, TODAY, "72.20");
        when(bodyProgressRepository.findByMember_IdAndRecordDate(101L, TODAY))
                .thenReturn(Optional.of(progress));

        BodyProgressResponse response = service.upsertCurrentProgress(
                principal,
                request(TODAY, "72.20")
        );

        verify(bodyProgressRepository).upsertAtomic(101L, TODAY, new BigDecimal("72.20"));
        assertThat(response.memberId()).isEqualTo(101L);
        assertThat(response.weightKg()).isEqualByComparingTo("72.20");
    }

    @Test
    @DisplayName("Ngày tương lai bị từ chối trước khi ghi database")
    void upsert_WithFutureDate_ShouldReject() {
        assertThatThrownBy(() -> service.upsertCurrentProgress(
                principal(101L, RoleName.ROLE_MEMBER),
                request(TODAY.plusDays(1), "72.20")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode().getCode()).isEqualTo("VAL-001"));

        verify(bodyProgressRepository, never()).upsertAtomic(any(), any(), any());
    }

    @Test
    @DisplayName("Ngày nghiệp vụ không phụ thuộc timezone mặc định của JVM")
    void upsert_WhenJvmTimezoneChanges_ShouldKeepBusinessDate() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
        BodyProgress progress = progress(101L, TODAY, "72.20");
        when(bodyProgressRepository.findByMember_IdAndRecordDate(101L, TODAY))
                .thenReturn(Optional.of(progress));

        service.upsertCurrentProgress(principal(101L, RoleName.ROLE_MEMBER), request(TODAY, "72.20"));

        verify(bodyProgressRepository).upsertAtomic(101L, TODAY, new BigDecimal("72.20"));
    }

    @Test
    @DisplayName("Cân nặng bằng 0 hoặc âm bị từ chối")
    void upsert_WithNonPositiveWeight_ShouldReject() {
        assertThatThrownBy(() -> service.upsertCurrentProgress(
                principal(101L, RoleName.ROLE_MEMBER), request(TODAY, "0.00")
        )).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.upsertCurrentProgress(
                principal(101L, RoleName.ROLE_MEMBER), request(TODAY, "-1.00")
        )).isInstanceOf(BusinessException.class);

        verify(bodyProgressRepository, never()).upsertAtomic(any(), any(), any());
    }

    @Test
    @DisplayName("History chỉ lấy Member hiện hành và map danh sách rỗng đúng")
    void getHistory_ShouldUsePrincipalOwner() {
        when(bodyProgressRepository.findByMember_IdOrderByRecordDateAsc(101L))
                .thenReturn(List.of(progress(101L, TODAY.minusDays(1), "71.80"), progress(101L, TODAY, "72.20")));

        List<BodyProgressResponse> history = service.getCurrentProgress(
                principal(101L, RoleName.ROLE_MEMBER)
        );

        verify(bodyProgressRepository).findByMember_IdOrderByRecordDateAsc(101L);
        assertThat(history).extracting(BodyProgressResponse::recordDate)
                .containsExactly(TODAY.minusDays(1), TODAY);
    }

    @Test
    @DisplayName("Guard chặn tài khoản không ACTIVE")
    void upsert_WhenAccountBlocked_ShouldNotWrite() {
        doThrow(new AccountStatusAccessDeniedException(AccountStatus.LOCKED))
                .when(accountStatusGuard).validateAccountStatusByUserId(101L);

        assertThatThrownBy(() -> service.upsertCurrentProgress(
                principal(101L, RoleName.ROLE_MEMBER), request(TODAY, "72.20")
        )).isInstanceOf(AccountStatusAccessDeniedException.class);

        verify(bodyProgressRepository, never()).upsertAtomic(any(), any(), any());
    }

    @Test
    @DisplayName("Principal không có ROLE_MEMBER bị từ chối")
    void upsert_WhenPrincipalIsAdmin_ShouldReject() {
        assertThatThrownBy(() -> service.upsertCurrentProgress(
                principal(101L, RoleName.ROLE_ADMIN), request(TODAY, "72.20")
        )).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    private BodyProgressUpsertRequest request(LocalDate date, String weight) {
        return new BodyProgressUpsertRequest(date, new BigDecimal(weight));
    }

    private BodyProgress progress(Long memberId, LocalDate date, String weight) {
        User user = user(memberId);
        BodyProgress progress = new BodyProgress(user, date, new BigDecimal(weight));
        progress.setId(memberId + date.getDayOfMonth());
        progress.setCreatedAt(Instant.parse("2026-08-01T00:00:00Z"));
        progress.setUpdatedAt(Instant.parse("2026-08-05T00:00:00Z"));
        return progress;
    }

    private AuthenticatedUserPrincipal principal(Long id, RoleName roleName) {
        return AuthenticatedUserPrincipal.from(user(id, roleName));
    }

    private User user(Long id, RoleName roleName) {
        User user = user(id);
        Role role = new Role(roleName);
        role.setId(roleName == RoleName.ROLE_ADMIN ? 1L : 2L);
        user.attachUserRole(new UserRole(user, role));
        return user;
    }

    private User user(Long id) {
        User user = new User("Progress Member", "progress@smartgym.test", "hash", AccountStatus.ACTIVE);
        user.setId(id);
        user.setCreatedAt(Instant.parse("2026-08-01T00:00:00Z"));
        return user;
    }
}
