package com.thinh.smartgym.member.service;

import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.member.dto.BioProfileResponse;
import com.thinh.smartgym.member.dto.MemberProfileResponse;
import com.thinh.smartgym.member.dto.NutritionProfileResponse;
import com.thinh.smartgym.member.entity.MemberProfile;
import com.thinh.smartgym.member.repository.MemberProfileRepository;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberProfileRepository memberProfileRepository;
    private final AccountStatusGuard accountStatusGuard;

    @Transactional(readOnly = true)
    public MemberProfileResponse getCurrentProfile(AuthenticatedUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        accountStatusGuard.validateAccountStatusByUserId(memberId);

        MemberProfile profile = memberProfileRepository.findByUser_Id(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        return toResponse(profile, memberId);
    }

    private Long requireMemberId(AuthenticatedUserPrincipal principal) {
        if (principal == null || principal.getId() == null || principal.getId() <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
        return principal.getId();
    }

    private MemberProfileResponse toResponse(MemberProfile profile, Long memberId) {
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

        return new MemberProfileResponse(memberId, bioProfile, nutritionProfile, profile.getUpdatedAt());
    }

    private Integer toInteger(Number value) {
        return value == null ? null : value.intValue();
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
