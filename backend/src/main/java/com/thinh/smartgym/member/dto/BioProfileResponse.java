package com.thinh.smartgym.member.dto;

import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.ContraindicationTag;
import com.thinh.smartgym.common.enums.Equipment;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.FitnessLevel;
import com.thinh.smartgym.common.enums.Gender;
import com.thinh.smartgym.common.enums.MuscleGroup;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Schema(description = "Thông tin sinh học và mục tiêu tập luyện của hội viên")
public record BioProfileResponse(
        @Schema(example = "MALE")
        Gender gender,
        @Schema(type = "string", format = "date", example = "1998-05-15")
        LocalDate dateOfBirth,
        @Schema(example = "175.00")
        BigDecimal heightCm,
        @Schema(example = "70.00")
        BigDecimal weightKg,
        @Schema(example = "BULK")
        FitnessGoal fitnessGoal,
        List<FitnessGoal> fitnessGoals,
        @Schema(example = "85.00", nullable = true)
        BigDecimal targetWeightKg,
        @Schema(example = "BEGINNER")
        FitnessLevel fitnessLevel,
        @Schema(example = "MODERATELY_ACTIVE")
        ActivityLevel activityLevel,
        @Schema(example = "4")
        Integer workoutDaysPerWeek,
        @Schema(example = "90")
        Integer maxSessionMinutes,
        Set<Equipment> availableEquipment,
        Set<MuscleGroup> targetMuscleGroups,
        Set<ContraindicationTag> injuryConstraints,
        @Schema(example = "Hạn chế xoay vai trái", nullable = true)
        String mobilityLimitNotes
) {

    public BioProfileResponse(
            Gender gender,
            LocalDate dateOfBirth,
            BigDecimal heightCm,
            BigDecimal weightKg,
            FitnessGoal fitnessGoal,
            FitnessLevel fitnessLevel,
            ActivityLevel activityLevel,
            Integer workoutDaysPerWeek,
            Integer maxSessionMinutes,
            Set<Equipment> availableEquipment,
            Set<MuscleGroup> targetMuscleGroups,
            Set<ContraindicationTag> injuryConstraints
    ) {
        this(
                gender, dateOfBirth, heightCm, weightKg, fitnessGoal,
                List.of(fitnessGoal), null, fitnessLevel, activityLevel,
                workoutDaysPerWeek, maxSessionMinutes, availableEquipment,
                targetMuscleGroups, injuryConstraints, null
        );
    }
}
