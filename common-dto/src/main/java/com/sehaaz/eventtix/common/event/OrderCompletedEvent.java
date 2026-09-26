package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public record OrderCompletedEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        Long userId,
        String userEmail,
        String eventTitle,
        int quantity
) implements SagaEvent {
}
