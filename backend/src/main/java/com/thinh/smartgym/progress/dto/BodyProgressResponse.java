package com.thinh.smartgym.progress.dto;

import com.thinh.smartgym.progress.entity.BodyProgress;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Bản ghi cân nặng của Member hiện hành")
public record BodyProgressResponse(
        @Schema(example = "305") Long id,
        @Schema(example = "101") Long memberId,
        @Schema(example = "2026-08-05") LocalDate recordDate,
        @Schema(example = "72.20") BigDecimal weightKg,
        @Schema(type = "string", format = "date-time") Instant createdAt,
        @Schema(type = "string", format = "date-time") Instant updatedAt
) {

    public static BodyProgressResponse from(BodyProgress progress) {
        return new BodyProgressResponse(
                progress.getId(),
                progress.getMember().getId(),
                progress.getRecordDate(),
                progress.getWeightKg(),
                progress.getCreatedAt(),
                progress.getUpdatedAt()
        );
    }
}
