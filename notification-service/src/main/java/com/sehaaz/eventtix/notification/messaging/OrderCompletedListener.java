package com.sehaaz.eventtix.notification.messaging;

import com.sehaaz.eventtix.common.event.OrderCompletedEvent;
import com.sehaaz.eventtix.notification.domain.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.NOTIFICATION_SUCCESS_QUEUE;

@Component
@RequiredArgsConstructor
public class OrderCompletedListener {

    private final MailService mailService;

    @RabbitListener(queues = NOTIFICATION_SUCCESS_QUEUE)
    public void onOrderCompleted(OrderCompletedEvent event) {
        mailService.send(event.userEmail(), "Biletleriniz hazır", "order-completed", Map.of(
                "orderId", event.sagaId(),
                "eventTitle", event.eventTitle(),
                "quantity", event.quantity()));
    }
}
