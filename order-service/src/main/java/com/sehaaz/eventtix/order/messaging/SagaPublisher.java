package com.sehaaz.eventtix.order.messaging;

import com.sehaaz.eventtix.common.event.SagaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.EXCHANGE;

@Component
@RequiredArgsConstructor
public class SagaPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Mesajı aktif transaction commit edildikten sonra yayınlar; rollback olursa mesaj gitmez.
     */
    public void publishAfterCommit(String routingKey, SagaEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            }
        });
    }
}
