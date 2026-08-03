package com.thinh.smartgym.member.service;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.ContraindicationTag;
import com.thinh.smartgym.common.enums.DietaryPreference;
import com.thinh.smartgym.common.enums.Equipment;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.FitnessLevel;
import com.thinh.smartgym.common.enums.Gender;
import com.thinh.smartgym.common.enums.MuscleGroup;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.member.entity.MemberProfile;
import com.thinh.smartgym.member.repository.MemberProfileRepository;
import com.thinh.smartgym.security.AccountStatusAccessDeniedException;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

    @Mock
    private MemberProfileRepository memberProfileRepository;

    @Mock
    private AccountStatusGuard accountStatusGuard;

    private MemberProfileService memberProfileService;

    @BeforeEach
    void setUp() {
        memberProfileService = new MemberProfileService(memberProfileRepository, accountStatusGuard);
    }

    @Test
    @DisplayName("Service dùng principal ID, gọi Guard trước repository và trả DTO đầy đủ")
    void getCurrentProfile_WithExistingProfile_ShouldReturnDtoFromPrincipalOwner() {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        MemberProfile profile = profile(101L);
        when(memberProfileRepository.findByUser_Id(101L)).thenReturn(Optional.of(profile));

        var response = memberProfileService.getCurrentProfile(principal);

        assertThat(response.memberId()).isEqualTo(101L);
        assertThat(response.bioProfile().gender()).isEqualTo(Gender.MALE);
        assertThat(response.bioProfile().availableEquipment())
                .containsExactly(Equipment.BARBELL, Equipment.CABLE, Equipment.DUMBBELL);
        assertThat(response.bioProfile().targetMuscleGroups())
                .containsExactly(MuscleGroup.BACK, MuscleGroup.CHEST, MuscleGroup.LEGS);
        assertThat(response.bioProfile().injuryConstraints())
                .containsExactly(ContraindicationTag.LOWER_BACK_LOAD_LIMITED);
        assertThat(response.nutritionProfile().foodAllergies()).containsExactly("PEANUTS");
        assertThat(response.nutritionProfile().excludedFoods()).containsExactly("BEEF");
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-08-03T03:30:00Z"));

        InOrder order = inOrder(accountStatusGuard, memberProfileRepository);
        order.verify(accountStatusGuard).validateAccountStatusByUserId(101L);
        order.verify(memberProfileRepository).findByUser_Id(101L);
    }

    @Test
    @DisplayName("Member chưa có Profile trả PROF-001")
    void getCurrentProfile_WithoutProfile_ShouldReturnProf001() {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        when(memberProfileRepository.findByUser_Id(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberProfileService.getCurrentProfile(principal))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROFILE_NOT_FOUND);
                    assertThat(exception.getDetails()).isEqualTo(java.util.Map.of());
                });

        verify(accountStatusGuard).validateAccountStatusByUserId(101L);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"LOCKED", "DISABLED"})
    @DisplayName("Guard chặn token cũ trước khi đọc Profile")
    void getCurrentProfile_WhenAccountBlocked_ShouldNotQueryProfile(AccountStatus status) {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        doThrow(new AccountStatusAccessDeniedException(status))
                .when(accountStatusGuard).validateAccountStatusByUserId(101L);

        assertThatThrownBy(() -> memberProfileService.getCurrentProfile(principal))
                .isInstanceOf(AccountStatusAccessDeniedException.class);

        verify(memberProfileRepository, never()).findByUser_Id(101L);
    }

    @Test
    @DisplayName("Principal thiếu identity trả SYS-001 trước mọi truy vấn")
    void getCurrentProfile_WithInvalidPrincipal_ShouldReject() {
        assertThatThrownBy(() -> memberProfileService.getCurrentProfile(null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_CONFIGURATION_ERROR));

        verifyNoInteractions(accountStatusGuard, memberProfileRepository);
    }

    private AuthenticatedUserPrincipal principal(Long id, AccountStatus status, RoleName roleName) {
        User user = new User("Gym Member", "member@smartgym.com", "password-hash", status);
        user.setId(id);
        user.setCreatedAt(Instant.parse("2026-08-01T03:00:00Z"));
        Role role = new Role(roleName);
        role.setId(2L);
        user.attachUserRole(new UserRole(user, role));
        return AuthenticatedUserPrincipal.from(user);
    }

    private MemberProfile profile(Long userId) {
        User user = new User("Gym Member", "member@smartgym.com", "password-hash", AccountStatus.ACTIVE);
        user.setId(userId);
        MemberProfile profile = new MemberProfile(
                user,
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                new BigDecimal("175.00"),
                new BigDecimal("70.00"),
                FitnessGoal.BULK,
                FitnessLevel.BEGINNER,
                ActivityLevel.MODERATELY_ACTIVE,
                (byte) 4,
                (short) 90,
                DietaryPreference.OMNIVORE,
                (byte) 4
        );
        profile.setId(501L);
        profile.setAvailableEquipment(Set.of(Equipment.DUMBBELL, Equipment.BARBELL, Equipment.CABLE));
        profile.setTargetMuscleGroups(Set.of(MuscleGroup.LEGS, MuscleGroup.CHEST, MuscleGroup.BACK));
        profile.setInjuryConstraints(Set.of(ContraindicationTag.LOWER_BACK_LOAD_LIMITED));
        profile.setFoodAllergies(Set.of("PEANUTS"));
        profile.setExcludedFoods(Set.of("BEEF"));
        profile.setUpdatedAt(Instant.parse("2026-08-03T03:30:00Z"));
        return profile;
    }
}
