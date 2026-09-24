package com.sehaaz.eventtix.ticket.messaging;

import com.sehaaz.eventtix.common.event.OrderConfirmedEvent;
import com.sehaaz.eventtix.common.event.TicketFailedEvent;
import com.sehaaz.eventtix.common.event.TicketGeneratedEvent;
import com.sehaaz.eventtix.ticket.domain.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.TICKET_FAILED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.TICKET_GENERATED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.TICKET_GENERATE_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedListener {

    private final TicketService ticketService;
    private final SagaPublisher publisher;
    private final Clock clock;

    // generate kendi transaction'ında çalışır; yayın ondan sonra yapıldığı için commit'ten sonradır.
    @RabbitListener(queues = TICKET_GENERATE_QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        List<String> codes;
        try {
            codes = ticketService.generate(event.sagaId(), event.userId(), event.eventId(), event.quantity());
        } catch (DataIntegrityViolationException e) {
            // Aynı mesaj eşzamanlı işlendi (uq_tickets_order): retry'da idempotency yolu mevcut kodları yayınlar.
            throw e;
        } catch (Exception e) {
            log.error("Sipariş {} için bilet üretilemedi", event.sagaId(), e);
            publisher.publish(TICKET_FAILED, new TicketFailedEvent(
                    event.sagaId(), TICKET_FAILED, clock.instant(), e.getMessage()));
            return;
        }
        publisher.publish(TICKET_GENERATED, new TicketGeneratedEvent(
                event.sagaId(), TICKET_GENERATED, clock.instant(), codes));
    }
}
