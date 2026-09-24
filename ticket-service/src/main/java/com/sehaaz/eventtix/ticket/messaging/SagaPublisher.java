package com.sehaaz.eventtix.ticket.messaging;

import com.sehaaz.eventtix.common.event.SagaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.EXCHANGE;

@Component
@RequiredArgsConstructor
public class SagaPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Transaction dışında çağrılır: bilet transaction'ı commit ya da rollback olduktan sonra.
     */
    public void publish(String routingKey, SagaEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
    }
}
