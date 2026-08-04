package com.thinh.smartgym.recommendation.calculator;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Các chỉ số định lượng do Backend sở hữu và tính toán. */
@Schema(description = "Các chỉ số thể chất và dinh dưỡng do Backend tính toán")
public record CalculatedTargets(
        @Schema(example = "23.67") BigDecimal bmi,
        @Schema(example = "1683.75") BigDecimal bmr,
        @Schema(example = "2609.81") BigDecimal tdee,
        @Schema(example = "2909.81") BigDecimal dailyCaloriesKcal,
        @Schema(example = "159.50") BigDecimal proteinGrams,
        @Schema(example = "80.83") BigDecimal fatGrams,
        @Schema(example = "386.09") BigDecimal carbGrams
) {
}
