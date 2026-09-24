package com.sehaaz.eventtix.common.event;

import java.time.Instant;

public interface SagaEvent {

    Long sagaId();

    String eventType();

    Instant occurredAt();
}
