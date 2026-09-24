package com.sehaaz.eventtix.event.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record EventRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotBlank @Size(max = 255) String venue,
        @NotBlank @Size(max = 100) String city,
        @NotNull Instant eventDate,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal price,
        @Min(1) int totalQuota,
        @Size(max = 500) String imageUrl
) {
}
