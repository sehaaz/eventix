package com.sehaaz.eventtix.order.messaging;

import com.sehaaz.eventtix.common.event.QuotaRejectedEvent;
import com.sehaaz.eventtix.common.event.QuotaReservedEvent;
import com.sehaaz.eventtix.order.domain.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_QUOTA_REJECTED_QUEUE;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_QUOTA_RESERVED_QUEUE;

@Component
@RequiredArgsConstructor
public class QuotaListener {

    private final OrderService orderService;

    @RabbitListener(queues = ORDER_QUOTA_RESERVED_QUEUE)
    public void onQuotaReserved(QuotaReservedEvent event) {
        orderService.onQuotaReserved(event.sagaId());
    }

    @RabbitListener(queues = ORDER_QUOTA_REJECTED_QUEUE)
    public void onQuotaRejected(QuotaRejectedEvent event) {
        orderService.onQuotaRejected(event.sagaId());
    }
}
