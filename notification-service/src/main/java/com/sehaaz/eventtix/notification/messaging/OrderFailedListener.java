package com.sehaaz.eventtix.notification.messaging;

import com.sehaaz.eventtix.common.event.OrderFailedEvent;
import com.sehaaz.eventtix.notification.domain.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.NOTIFICATION_FAILURE_QUEUE;

@Component
@RequiredArgsConstructor
public class OrderFailedListener {

    private static final String QUOTA = "QUOTA";

    private final MailService mailService;

    // reason: QUOTA (kontenjan yetersiz) veya TICKET (bilet üretimi başarısız).
    @RabbitListener(queues = NOTIFICATION_FAILURE_QUEUE)
    public void onOrderFailed(OrderFailedEvent event) {
        boolean quota = QUOTA.equals(event.reason());
        mailService.send(event.userEmail(),
                quota ? "Kontenjan yetersiz" : "Bilet oluşturulamadı",
                quota ? "order-failed-quota" : "order-failed-ticket",
                Map.of("orderId", event.sagaId()));
    }
}
