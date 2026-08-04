package com.thinh.smartgym.member.service;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.member.dto.BioProfileResponse;
import com.thinh.smartgym.member.dto.CalculatedTargetsResponse;
import com.thinh.smartgym.member.dto.MemberProfileResponse;
import com.thinh.smartgym.member.dto.MemberProfileUpsertRequest;
import com.thinh.smartgym.member.dto.NutritionProfileResponse;
import com.thinh.smartgym.member.entity.MemberProfile;
import com.thinh.smartgym.member.repository.MemberProfileRepository;
import com.thinh.smartgym.recommendation.calculator.BiometricCalculationService;
import com.thinh.smartgym.recommendation.calculator.CalculatedTargets;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MemberProfileRepository memberProfileRepository;
    private final UserRepository userRepository;
    private final AccountStatusGuard accountStatusGuard;
    private final BiometricCalculationService biometricCalculationService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MemberProfileResponse getCurrentProfile(AuthenticatedUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        accountStatusGuard.validateAccountStatusByUserId(memberId);

        MemberProfile profile = memberProfileRepository.findByUser_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        return toResponse(profile, memberId);
    }

    @Transactional
    public MemberProfileResponse upsertCurrentProfile(
            AuthenticatedUserPrincipal principal,
            MemberProfileUpsertRequest request
    ) {
        Long memberId = requireMemberId(principal);
        accountStatusGuard.validateAccountStatusByUserId(memberId);
        validateDateOfBirth(request.dateOfBirth());

        Set<String> foodAllergies = sanitizeTextSet(request.foodAllergies());
        Set<String> excludedFoods = sanitizeTextSet(request.excludedFoods());
        MemberProfile profile = memberProfileRepository.findByUser_Id(memberId)
                .orElseGet(() -> newProfile(memberId, request));

        profile.updateFrom(
                request.gender(),
                request.dateOfBirth(),
                request.heightCm(),
                request.weightKg(),
                request.fitnessGoal(),
                request.fitnessLevel(),
                request.activityLevel(),
                request.workoutDaysPerWeek().byteValue(),
                request.maxSessionMinutes().shortValue(),
                request.dietaryPreference(),
                request.mealsPerDay().byteValue(),
                request.availableEquipment(),
                request.targetMuscleGroups(),
                request.injuryConstraints(),
                foodAllergies,
                excludedFoods
        );

        MemberProfile saved = memberProfileRepository.save(profile);
        return toResponse(saved, memberId);
    }

    private MemberProfile newProfile(Long memberId, MemberProfileUpsertRequest request) {
        User owner = userRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR));
        return new MemberProfile(
                owner,
                request.gender(),
                request.dateOfBirth(),
                request.heightCm(),
                request.weightKg(),
                request.fitnessGoal(),
                request.fitnessLevel(),
                request.activityLevel(),
                request.workoutDaysPerWeek().byteValue(),
                request.maxSessionMinutes().shortValue(),
                request.dietaryPreference(),
                request.mealsPerDay().byteValue()
        );
    }

    private void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth.isAfter(LocalDate.now(clock.withZone(BUSINESS_ZONE)))) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    java.util.Map.of("field", "dateOfBirth", "constraint", "Ngày sinh không được ở tương lai.")
            );
        }
    }

    private Set<String> sanitizeTextSet(Set<String> values) {
        return values.stream()
                .map(this::removeControlCharacters)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String removeControlCharacters(String value) {
        return value.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private Long requireMemberId(AuthenticatedUserPrincipal principal) {
        if (principal == null || principal.getId() == null || principal.getId() <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
        return principal.getId();
    }

    private MemberProfileResponse toResponse(MemberProfile profile, Long memberId) {
        CalculatedTargets targets = calculateTargets(profile);

        BioProfileResponse bioProfile = new BioProfileResponse(
                profile.getGender(),
                profile.getDateOfBirth(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getFitnessGoal(),
                profile.getFitnessLevel(),
                profile.getActivityLevel(),
                toInteger(profile.getWorkoutDaysPerWeek()),
                toInteger(profile.getMaxSessionMinutes()),
                immutableSortedSet(profile.getAvailableEquipment(), Enum::name),
                immutableSortedSet(profile.getTargetMuscleGroups(), Enum::name),
                immutableSortedSet(profile.getInjuryConstraints(), Enum::name)
        );

        NutritionProfileResponse nutritionProfile = new NutritionProfileResponse(
                profile.getDietaryPreference(),
                immutableSortedSet(profile.getFoodAllergies(), Function.identity()),
                immutableSortedSet(profile.getExcludedFoods(), Function.identity()),
                toInteger(profile.getMealsPerDay())
        );

        return new MemberProfileResponse(
                memberId,
                bioProfile,
                nutritionProfile,
                CalculatedTargetsResponse.from(targets),
                profile.getUpdatedAt()
        );
    }

    private Integer toInteger(Number value) {
        return value == null ? null : value.intValue();
    }

    private CalculatedTargets calculateTargets(MemberProfile profile) {
        try {
            return biometricCalculationService.calculate(
                    profile.getGender(),
                    profile.getDateOfBirth(),
                    profile.getHeightCm(),
                    profile.getWeightKg(),
                    profile.getActivityLevel(),
                    profile.getFitnessGoal()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    java.util.Map.of(
                            "field", "profile",
                            "constraint", "Dữ liệu Profile không thể tạo bộ chỉ số dinh dưỡng hợp lệ."
                    )
            );
        }
    }

    private <T, U extends Comparable<? super U>> Set<T> immutableSortedSet(
            Set<T> source,
            Function<T, U> sortKey
    ) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<T> ordered = source.stream()
                .sorted(Comparator.comparing(sortKey))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(ordered);
    }
}
