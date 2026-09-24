package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public record OrderCreatedEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        Long userId,
        Long eventId,
        int quantity
) implements SagaEvent {
}
