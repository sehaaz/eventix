package com.sehaaz.eventtix.event.api.dto;

import com.sehaaz.eventtix.event.domain.Event;

import java.math.BigDecimal;
import java.time.Instant;

public record EventResponse(
        Long id,
        String title,
        String description,
        String venue,
        String city,
        Instant eventDate,
        BigDecimal price,
        int totalQuota,
        int soldCount,
        String imageUrl
) {

    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getVenue(),
                event.getCity(), event.getEventDate(), event.getPrice(), event.getTotalQuota(),
                event.getSoldCount(), event.getImageUrl());
    }
}
