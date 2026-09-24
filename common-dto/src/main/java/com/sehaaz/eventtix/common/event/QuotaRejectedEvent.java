package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public record QuotaRejectedEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        Long eventId,
        String reason
) implements SagaEvent {
}
