package com.thinh.smartgym.member.dto;

import com.thinh.smartgym.recommendation.calculator.CalculatedTargets;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Các chỉ số được Backend tính toán từ Profile")
public record CalculatedTargetsResponse(
        BigDecimal bmi,
        String bmiCategory,
        BigDecimal bmr,
        BigDecimal tdee,
        BigDecimal dailyCaloriesKcal,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        BigDecimal carbGrams
) {

    public static CalculatedTargetsResponse from(CalculatedTargets targets) {
        return new CalculatedTargetsResponse(
                targets.bmi(),
                bmiCategory(targets.bmi()),
                targets.bmr(),
                targets.tdee(),
                targets.dailyCaloriesKcal(),
                targets.proteinGrams(),
                targets.fatGrams(),
                targets.carbGrams()
        );
    }

    private static String bmiCategory(BigDecimal bmi) {
        if (bmi.compareTo(BigDecimal.valueOf(18.5)) < 0) return "UNDERWEIGHT";
        if (bmi.compareTo(BigDecimal.valueOf(25)) < 0) return "NORMAL";
        if (bmi.compareTo(BigDecimal.valueOf(30)) < 0) return "OVERWEIGHT";
        return "OBESE";
    }

    public CalculatedTargetsResponse(
            BigDecimal bmi,
            BigDecimal bmr,
            BigDecimal tdee,
            BigDecimal dailyCaloriesKcal,
            BigDecimal proteinGrams,
            BigDecimal fatGrams,
            BigDecimal carbGrams
    ) {
        this(bmi, bmiCategory(bmi), bmr, tdee, dailyCaloriesKcal, proteinGrams, fatGrams, carbGrams);
    }
}
