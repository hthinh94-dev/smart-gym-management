package com.thinh.smartgym.member.service;

import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.UserRepository;
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
import com.thinh.smartgym.member.dto.MemberProfileUpsertRequest;
import com.thinh.smartgym.member.repository.MemberProfileRepository;
import com.thinh.smartgym.recommendation.calculator.BiometricCalculationService;
import com.thinh.smartgym.recommendation.calculator.CalculatedTargets;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.any;
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
    private UserRepository userRepository;

    @Mock
    private AccountStatusGuard accountStatusGuard;

    @Mock
    private BiometricCalculationService biometricCalculationService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC);

    private MemberProfileService memberProfileService;

    @BeforeEach
    void setUp() {
        memberProfileService = new MemberProfileService(
                memberProfileRepository,
                userRepository,
                accountStatusGuard,
                biometricCalculationService,
                clock
        );
    }

    @Test
    @DisplayName("Service dùng principal ID, gọi Guard trước repository và trả DTO đầy đủ")
    void getCurrentProfile_WithExistingProfile_ShouldReturnDtoFromPrincipalOwner() {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        MemberProfile profile = profile(101L);
        when(memberProfileRepository.findByUser_Id(101L)).thenReturn(Optional.of(profile));
        stubTargets();

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
        assertThat(response.calculatedTargets().dailyCaloriesKcal()).isEqualByComparingTo("2909.81");
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

        verifyNoInteractions(accountStatusGuard, memberProfileRepository, userRepository);
    }

    @Test
    @DisplayName("PUT tạo Profile mới với owner từ principal và sanitize collection")
    void upsertCurrentProfile_WithoutExistingProfile_ShouldCreateForPrincipalOwner() {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        User owner = user(101L);
        MemberProfileUpsertRequest request = request(
                Set.of("  PEANUTS  ", String.valueOf((char) 1), "PEANUTS"),
                Set.of("  BEEF" + (char) 7 + " ", "   ")
        );
        when(memberProfileRepository.findByUser_Id(101L)).thenReturn(Optional.empty());
        when(userRepository.findById(101L)).thenReturn(Optional.of(owner));
        when(memberProfileRepository.save(any(MemberProfile.class))).thenAnswer(invocation -> {
            MemberProfile saved = invocation.getArgument(0);
            saved.setId(501L);
            saved.setUpdatedAt(Instant.parse("2026-08-04T03:30:00Z"));
            return saved;
        });
        stubTargets();

        var response = memberProfileService.upsertCurrentProfile(principal, request);

        assertThat(response.memberId()).isEqualTo(101L);
        assertThat(response.nutritionProfile().foodAllergies()).containsExactly("PEANUTS");
        assertThat(response.nutritionProfile().excludedFoods()).containsExactly("BEEF");
        verify(memberProfileRepository).save(org.mockito.ArgumentMatchers.argThat(profile ->
                profile.getUser() == owner && profile.getUser().getId().equals(101L)));
    }

    @Test
    @DisplayName("PUT cập nhật đúng Profile hiện có và không đổi owner")
    void upsertCurrentProfile_WithExistingProfile_ShouldUpdateWithoutChangingOwner() {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        MemberProfile existing = profile(101L);
        User originalOwner = existing.getUser();
        MemberProfileUpsertRequest request = request(Set.of("MILK"), Set.of());
        when(memberProfileRepository.findByUser_Id(101L)).thenReturn(Optional.of(existing));
        when(memberProfileRepository.save(existing)).thenReturn(existing);
        stubTargets();

        var response = memberProfileService.upsertCurrentProfile(principal, request);

        assertThat(existing.getUser()).isSameAs(originalOwner);
        assertThat(existing.getFoodAllergies()).containsExactly("MILK");
        assertThat(response.memberId()).isEqualTo(101L);
        verify(userRepository, never()).findById(any());
        verify(memberProfileRepository, never()).findByUser_Id(202L);
    }

    @Test
    @DisplayName("PUT ngày sinh tương lai trả VAL-001 trước repository")
    void upsertCurrentProfile_WithFutureDateOfBirth_ShouldReject() {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        MemberProfileUpsertRequest invalid = new MemberProfileUpsertRequest(
                Gender.MALE,
                LocalDate.of(2026, 8, 5),
                new BigDecimal("175.00"),
                new BigDecimal("72.50"),
                FitnessGoal.BULK,
                FitnessLevel.BEGINNER,
                ActivityLevel.MODERATELY_ACTIVE,
                4,
                90,
                Set.of(Equipment.BARBELL),
                Set.of(MuscleGroup.CHEST),
                Set.of(),
                DietaryPreference.OMNIVORE,
                Set.of(),
                Set.of(),
                4
        );

        assertThatThrownBy(() -> memberProfileService.upsertCurrentProfile(principal, invalid))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(memberProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lỗi save được truyền ra và Calculator không chạy sau thất bại")
    void upsertCurrentProfile_WhenSaveFails_ShouldNotCalculateResponse() {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        MemberProfile existing = profile(101L);
        when(memberProfileRepository.findByUser_Id(101L)).thenReturn(Optional.of(existing));
        when(memberProfileRepository.save(existing)).thenThrow(new IllegalStateException("database failure"));
        assertThatThrownBy(() -> memberProfileService.upsertCurrentProfile(
                principal,
                request(Set.of(), Set.of())
        )).isInstanceOf(IllegalStateException.class);

        verify(biometricCalculationService, never()).calculate(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"LOCKED", "DISABLED"})
    @DisplayName("Guard chặn PUT trước khi truy vấn hoặc lưu Profile")
    void upsertCurrentProfile_WhenAccountBlocked_ShouldNotWrite(AccountStatus status) {
        AuthenticatedUserPrincipal principal = principal(101L, AccountStatus.ACTIVE, RoleName.ROLE_MEMBER);
        doThrow(new AccountStatusAccessDeniedException(status))
                .when(accountStatusGuard).validateAccountStatusByUserId(101L);

        assertThatThrownBy(() -> memberProfileService.upsertCurrentProfile(
                principal,
                request(Set.of(), Set.of())
        )).isInstanceOf(AccountStatusAccessDeniedException.class);

        verifyNoInteractions(memberProfileRepository, userRepository, biometricCalculationService);
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
        User user = user(userId);
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

    private User user(Long userId) {
        User user = new User("Gym Member", "member@smartgym.com", "password-hash", AccountStatus.ACTIVE);
        user.setId(userId);
        return user;
    }

    private MemberProfileUpsertRequest request(Set<String> allergies, Set<String> excludedFoods) {
        return new MemberProfileUpsertRequest(
                Gender.MALE,
                LocalDate.of(1998, 5, 15),
                new BigDecimal("175.00"),
                new BigDecimal("72.50"),
                FitnessGoal.BULK,
                FitnessLevel.BEGINNER,
                ActivityLevel.MODERATELY_ACTIVE,
                4,
                90,
                Set.of(Equipment.BARBELL, Equipment.DUMBBELL),
                Set.of(MuscleGroup.CHEST, MuscleGroup.BACK),
                Set.of(ContraindicationTag.LOWER_BACK_LOAD_LIMITED),
                DietaryPreference.OMNIVORE,
                allergies,
                excludedFoods,
                4
        );
    }

    private CalculatedTargets targets() {
        return new CalculatedTargets(
                new BigDecimal("23.67"),
                new BigDecimal("1683.75"),
                new BigDecimal("2609.81"),
                new BigDecimal("2909.81"),
                new BigDecimal("159.50"),
                new BigDecimal("80.83"),
                new BigDecimal("386.09")
        );
    }

    private void stubTargets() {
        when(biometricCalculationService.calculate(any(), any(), any(), any(), any(), any()))
                .thenReturn(targets());
    }
}
