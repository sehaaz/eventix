package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public record TicketFailedEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        String reason
) implements SagaEvent {
}
