package com.thinh.smartgym.recommendation.calculator;

import com.thinh.smartgym.common.enums.ActivityLevel;
import com.thinh.smartgym.common.enums.FitnessGoal;
import com.thinh.smartgym.common.enums.Gender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Objects;

/** Calculator thuần, không truy cập repository hoặc trạng thái request. */
@Service
public class BiometricCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal CALORIES_PER_GRAM = BigDecimal.valueOf(4);
    private static final BigDecimal FAT_CALORIES_PER_GRAM = BigDecimal.valueOf(9);
    private static final BigDecimal PROTEIN_GRAMS_PER_KG = BigDecimal.valueOf(2.2);
    private static final BigDecimal FAT_CALORIE_RATIO = BigDecimal.valueOf(0.25);
    private static final BigDecimal BULK_SURPLUS = BigDecimal.valueOf(300);
    private static final BigDecimal CUT_DEFICIT = BigDecimal.valueOf(500);
    private static final int SCALE = 2;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Clock clock;

    public BiometricCalculationService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public CalculatedTargets calculate(
            Gender gender,
            LocalDate dateOfBirth,
            BigDecimal heightCm,
            BigDecimal weightKg,
            ActivityLevel activityLevel,
            FitnessGoal fitnessGoal
    ) {
        validateInput(gender, dateOfBirth, heightCm, weightKg, activityLevel, fitnessGoal);

        int age = Period.between(dateOfBirth, currentBusinessDate()).getYears();
        BigDecimal height = heightCm.setScale(2, RoundingMode.HALF_UP);
        BigDecimal weight = weightKg.setScale(2, RoundingMode.HALF_UP);

        BigDecimal bmi = weight.divide(height.divide(HUNDRED, 8, RoundingMode.HALF_UP)
                        .pow(2), 8, RoundingMode.HALF_UP);
        BigDecimal bmr = BigDecimal.TEN.multiply(weight)
                .add(BigDecimal.valueOf(6.25).multiply(height))
                .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(age)))
                .add(gender == Gender.MALE ? BigDecimal.valueOf(5) : BigDecimal.valueOf(-161));
        BigDecimal tdee = bmr.multiply(activityMultiplier(activityLevel));
        BigDecimal dailyCalories;
        if (fitnessGoal.requiresCalorieSurplus()) {
            dailyCalories = tdee.add(BULK_SURPLUS);
        } else if (fitnessGoal.requiresCalorieDeficit()) {
            dailyCalories = tdee.subtract(CUT_DEFICIT);
        } else {
            dailyCalories = tdee;
        }

        BigDecimal protein = PROTEIN_GRAMS_PER_KG.multiply(weight);
        BigDecimal fat = dailyCalories.multiply(FAT_CALORIE_RATIO).divide(FAT_CALORIES_PER_GRAM,
                8, RoundingMode.HALF_UP);
        BigDecimal carb = dailyCalories.subtract(protein.multiply(CALORIES_PER_GRAM))
                .subtract(fat.multiply(FAT_CALORIES_PER_GRAM))
                .divide(CALORIES_PER_GRAM, 8, RoundingMode.HALF_UP);

        if (carb.signum() < 0) {
            throw new IllegalArgumentException("Calculated carb calories must not be negative");
        }

        return new CalculatedTargets(
                round(bmi),
                round(bmr),
                round(tdee),
                round(dailyCalories),
                round(protein),
                round(fat),
                round(carb)
        );
    }

    private void validateInput(
            Gender gender,
            LocalDate dateOfBirth,
            BigDecimal heightCm,
            BigDecimal weightKg,
            ActivityLevel activityLevel,
            FitnessGoal fitnessGoal
    ) {
        LocalDate today = currentBusinessDate();
        if (gender == null || dateOfBirth == null || heightCm == null || weightKg == null
                || activityLevel == null || fitnessGoal == null) {
            throw new IllegalArgumentException("Calculator input must not be null");
        }
        if (dateOfBirth.isAfter(today)) {
            throw new IllegalArgumentException("dateOfBirth must not be in the future");
        }
        if (heightCm.signum() <= 0 || weightKg.signum() <= 0) {
            throw new IllegalArgumentException("heightCm and weightKg must be positive");
        }
    }

    private LocalDate currentBusinessDate() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }

    private BigDecimal activityMultiplier(ActivityLevel activityLevel) {
        return switch (activityLevel) {
            case SEDENTARY -> BigDecimal.valueOf(1.2);
            case LIGHTLY_ACTIVE -> BigDecimal.valueOf(1.375);
            case MODERATELY_ACTIVE -> BigDecimal.valueOf(1.55);
            case VERY_ACTIVE -> BigDecimal.valueOf(1.725);
        };
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
