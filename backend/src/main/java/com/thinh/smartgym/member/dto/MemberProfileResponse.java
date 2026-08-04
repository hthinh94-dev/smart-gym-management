package com.thinh.smartgym.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Hồ sơ thể trạng và dinh dưỡng của hội viên hiện hành")
public record MemberProfileResponse(
        @Schema(description = "ID User lấy từ principal hiện hành", example = "101")
        Long memberId,
        BioProfileResponse bioProfile,
        NutritionProfileResponse nutritionProfile,
        CalculatedTargetsResponse calculatedTargets,
        @Schema(type = "string", format = "date-time", example = "2026-08-03T10:30:00Z")
        Instant updatedAt
) {
}
