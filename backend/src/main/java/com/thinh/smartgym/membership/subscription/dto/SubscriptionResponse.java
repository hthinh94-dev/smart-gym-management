package com.thinh.smartgym.membership.subscription.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thinh.smartgym.membership.subscription.entity.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionResponse(
        Long subscriptionId,
        Long memberId,
        Long packageId,
        String packageName,
        BigDecimal price,
        SubscriptionStatus status,
        Instant requestedAt,
        LocalDate startDate,
        LocalDate endDate,
        Long daysRemaining,
        Instant approvedAt
) {
}
