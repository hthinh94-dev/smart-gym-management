package com.thinh.smartgym.membership.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSubscriptionRequest(
        @NotNull(message = "Gói tập là bắt buộc.")
        @Positive(message = "ID gói tập phải lớn hơn 0.")
        Long packageId
) {
}
