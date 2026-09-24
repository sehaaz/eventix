package com.sehaaz.eventtix.event.messaging;

import com.sehaaz.eventtix.common.event.OrderCancelledEvent;
import com.sehaaz.eventtix.event.domain.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.EVENT_QUOTA_RELEASE_QUEUE;

@Component
@RequiredArgsConstructor
public class OrderCancelledListener {

    private final EventService eventService;

    // releaseQuota rezervasyon kaydını silerek iade eder; tekrar gelen mesajda kayıt yok → no-op.
    @RabbitListener(queues = EVENT_QUOTA_RELEASE_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event) {
        eventService.releaseQuota(event.sagaId());
    }
}
