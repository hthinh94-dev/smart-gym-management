package com.thinh.smartgym.member.dto;

import com.thinh.smartgym.common.enums.DietaryPreference;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Sở thích và giới hạn dinh dưỡng của hội viên")
public record NutritionProfileResponse(
        @Schema(example = "OMNIVORE")
        DietaryPreference dietaryPreference,
        @Schema(example = "[\"PEANUTS\"]")
        Set<String> foodAllergies,
        @Schema(example = "[\"BEEF\"]")
        Set<String> excludedFoods,
        @Schema(example = "4")
        Integer mealsPerDay
) {
}
