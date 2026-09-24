package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public record QuotaReservedEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        Long eventId,
        int quantity
) implements SagaEvent {
}
