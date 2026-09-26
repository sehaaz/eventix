package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public record OrderFailedEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        Long userId,
        String userEmail,
        String reason
) implements SagaEvent {
}
