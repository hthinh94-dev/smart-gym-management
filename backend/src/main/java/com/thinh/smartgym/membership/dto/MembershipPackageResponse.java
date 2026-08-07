package com.thinh.smartgym.membership.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MembershipPackageResponse(
        Long id,
        String name,
        Integer durationDays,
        BigDecimal price,
        String description,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
