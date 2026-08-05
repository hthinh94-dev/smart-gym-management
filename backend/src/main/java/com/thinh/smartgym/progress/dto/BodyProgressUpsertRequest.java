package com.thinh.smartgym.progress.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Dữ liệu ghi nhận cân nặng theo ngày")
public record BodyProgressUpsertRequest(
        @NotNull(message = "Ngày ghi nhận là bắt buộc.")
        @Schema(example = "2026-08-05")
        LocalDate recordDate,
        @NotNull(message = "Cân nặng là bắt buộc.")
        @Positive(message = "Cân nặng phải lớn hơn 0.")
        @Digits(integer = 4, fraction = 2, message = "Cân nặng tối đa 4 chữ số nguyên và 2 chữ số thập phân.")
        @Schema(example = "72.20")
        BigDecimal weightKg
) {
}
