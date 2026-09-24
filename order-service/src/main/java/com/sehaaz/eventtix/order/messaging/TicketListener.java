package com.sehaaz.eventtix.order.messaging;

import com.sehaaz.eventtix.common.event.TicketFailedEvent;
import com.sehaaz.eventtix.common.event.TicketGeneratedEvent;
import com.sehaaz.eventtix.order.domain.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_TICKET_FAILED_QUEUE;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_TICKET_GENERATED_QUEUE;

@Component
@RequiredArgsConstructor
public class TicketListener {

    private final OrderService orderService;

    @RabbitListener(queues = ORDER_TICKET_GENERATED_QUEUE)
    public void onTicketGenerated(TicketGeneratedEvent event) {
        orderService.onTicketGenerated(event.sagaId());
    }

    @RabbitListener(queues = ORDER_TICKET_FAILED_QUEUE)
    public void onTicketFailed(TicketFailedEvent event) {
        orderService.onTicketFailed(event.sagaId());
    }
}
