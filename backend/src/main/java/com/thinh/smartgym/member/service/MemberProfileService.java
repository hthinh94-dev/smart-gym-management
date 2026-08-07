package com.thinh.smartgym.member.service;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.FitnessGoal;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        List<FitnessGoal> goals = resolveGoals(request);
        validateGoalPlan(request.weightKg(), request.targetWeightKg(), goals);

        Set<String> foodAllergies = sanitizeTextSet(request.foodAllergies());
        Set<String> excludedFoods = sanitizeTextSet(request.excludedFoods());
        MemberProfile profile = memberProfileRepository.findByUser_Id(memberId)
                .orElseGet(() -> newProfile(memberId, request, goals));

        profile.updateFrom(
                request.gender(),
                request.dateOfBirth(),
                request.heightCm(),
                request.weightKg(),
                request.targetWeightKg(),
                request.fitnessGoal(),
                Set.copyOf(goals),
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
                excludedFoods,
                sanitizeOptionalText(request.mobilityLimitNotes())
        );

        MemberProfile saved = memberProfileRepository.save(profile);
        return toResponse(saved, memberId);
    }

    private MemberProfile newProfile(
            Long memberId,
            MemberProfileUpsertRequest request,
            List<FitnessGoal> goals
    ) {
        User owner = userRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR));
        return new MemberProfile(
                owner,
                request.gender(),
                request.dateOfBirth(),
                request.heightCm(),
                request.weightKg(),
                request.targetWeightKg(),
                request.fitnessGoal(),
                Set.copyOf(goals),
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
                    Map.of("field", "dateOfBirth", "constraint", "Ngày sinh không được ở tương lai.")
            );
        }
    }

    private List<FitnessGoal> resolveGoals(MemberProfileUpsertRequest request) {
        List<FitnessGoal> goals = request.fitnessGoals() == null
                || request.fitnessGoals().isEmpty()
                ? List.of(request.fitnessGoal())
                : request.fitnessGoals().stream().distinct().toList();
        if (goals.isEmpty() || goals.size() > 2 || goals.stream().anyMatch(Objects::isNull)) {
            throw validation("fitnessGoals", "Bạn chỉ có thể chọn từ 1 đến 2 mục tiêu.");
        }
        if (!goals.contains(request.fitnessGoal())) {
            throw validation("fitnessGoal", "Mục tiêu chính phải nằm trong danh sách mục tiêu.");
        }
        return goals;
    }

    private void validateGoalPlan(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            List<FitnessGoal> goals
    ) {
        boolean gainingWeight = goals.contains(FitnessGoal.WEIGHT_GAIN);
        boolean losingWeight = goals.contains(FitnessGoal.WEIGHT_LOSS);
        if (gainingWeight && losingWeight) {
            throw validation("fitnessGoals", "Không thể đồng thời chọn tăng cân và giảm cân.");
        }
        if ((gainingWeight || losingWeight) && targetWeight == null) {
            throw validation("targetWeightKg", "Mục tiêu tăng/giảm cân bắt buộc có cân nặng đích.");
        }
        if (targetWeight == null) {
            return;
        }
        if (gainingWeight && targetWeight.compareTo(currentWeight) <= 0) {
            throw validation("targetWeightKg", "Cân nặng mục tiêu phải cao hơn cân nặng hiện tại.");
        }
        if (losingWeight && targetWeight.compareTo(currentWeight) >= 0) {
            throw validation("targetWeightKg", "Cân nặng mục tiêu phải thấp hơn cân nặng hiện tại.");
        }
    }

    private BusinessException validation(String field, String constraint) {
        return new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                Map.of("field", field, "constraint", constraint)
        );
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

    private String sanitizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = removeControlCharacters(value).trim();
        return normalized.isEmpty() ? null : normalized;
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
                orderedGoals(profile),
                profile.getTargetWeightKg(),
                profile.getFitnessLevel(),
                profile.getActivityLevel(),
                toInteger(profile.getWorkoutDaysPerWeek()),
                toInteger(profile.getMaxSessionMinutes()),
                immutableSortedSet(profile.getAvailableEquipment(), Enum::name),
                immutableSortedSet(profile.getTargetMuscleGroups(), Enum::name),
                immutableSortedSet(profile.getInjuryConstraints(), Enum::name),
                profile.getMobilityLimitNotes()
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
                    Map.of(
                            "field", "profile",
                            "constraint", "Dữ liệu Profile không thể tạo bộ chỉ số dinh dưỡng hợp lệ."
                    )
            );
        }
    }

    private List<FitnessGoal> orderedGoals(MemberProfile profile) {
        Set<FitnessGoal> source = profile.getFitnessGoals();
        List<FitnessGoal> ordered = new ArrayList<>();
        FitnessGoal primary = profile.getFitnessGoal();
        if (primary != null && (source == null || source.isEmpty() || source.contains(primary))) {
            ordered.add(primary);
        }
        if (source != null) {
            source.stream()
                    .filter(goal -> !Objects.equals(goal, primary))
                    .sorted(Comparator.comparing(Enum::name))
                    .forEach(ordered::add);
        }
        return List.copyOf(ordered);
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
