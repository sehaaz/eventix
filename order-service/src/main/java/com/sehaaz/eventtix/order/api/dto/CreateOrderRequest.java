package com.sehaaz.eventtix.order.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(@NotNull Long eventId, @Positive int quantity) {
}
