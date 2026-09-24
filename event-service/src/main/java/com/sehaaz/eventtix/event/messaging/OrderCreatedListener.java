package com.sehaaz.eventtix.event.messaging;

import com.sehaaz.eventtix.common.event.OrderCreatedEvent;
import com.sehaaz.eventtix.common.event.QuotaRejectedEvent;
import com.sehaaz.eventtix.common.event.QuotaReservedEvent;
import com.sehaaz.eventtix.event.domain.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.EVENT_QUOTA_QUEUE;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.QUOTA_REJECTED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.QUOTA_RESERVED;

@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final EventService eventService;
    private final SagaPublisher publisher;
    private final Clock clock;

    // reserveQuota aynı orderId için tekrar çağrılırsa yeniden ayırmaz, true döner → QuotaReserved tekrar yayınlanır.
    @Transactional
    @RabbitListener(queues = EVENT_QUOTA_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (eventService.reserveQuota(event.sagaId(), event.eventId(), event.quantity())) {
            publisher.publishAfterCommit(QUOTA_RESERVED, new QuotaReservedEvent(
                    event.sagaId(), QUOTA_RESERVED, clock.instant(), event.eventId(), event.quantity()));
        } else {
            publisher.publishAfterCommit(QUOTA_REJECTED, new QuotaRejectedEvent(
                    event.sagaId(), QUOTA_REJECTED, clock.instant(), event.eventId(), "QUOTA"));
        }
    }
}
