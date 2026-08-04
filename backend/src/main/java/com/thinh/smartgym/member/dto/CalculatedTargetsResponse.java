package com.thinh.smartgym.member.dto;

import com.thinh.smartgym.recommendation.calculator.CalculatedTargets;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Các chỉ số được Backend tính toán từ Profile")
public record CalculatedTargetsResponse(
        BigDecimal bmi,
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
                targets.bmr(),
                targets.tdee(),
                targets.dailyCaloriesKcal(),
                targets.proteinGrams(),
                targets.fatGrams(),
                targets.carbGrams()
        );
    }
}
