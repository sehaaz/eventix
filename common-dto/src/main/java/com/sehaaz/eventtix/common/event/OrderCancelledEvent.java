package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public record OrderCancelledEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        Long userId,
        Long eventId,
        int quantity,
        String reason
) implements SagaEvent {
}
