package com.sehaaz.eventtix.common.event;

import java.time.Instant;
import java.util.List;

public record TicketGeneratedEvent(
        Long sagaId,
        String eventType,
        Instant occurredAt,
        List<String> ticketCodes
) implements SagaEvent {
}
