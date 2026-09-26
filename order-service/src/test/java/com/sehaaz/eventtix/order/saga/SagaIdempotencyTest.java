package com.sehaaz.eventtix.order.saga;

import com.sehaaz.eventtix.common.event.OrderCreatedEvent;
import com.sehaaz.eventtix.order.domain.Order;
import com.sehaaz.eventtix.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.EXCHANGE;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SagaIdempotencyTest extends SagaTestSupport {

    private static final int QUANTITY = 2;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void duplicateOrderCreatedEvent_reservesQuotaOnlyOnce() {
        Long eventId = createEvent(10, 0);
        Long orderId = saveOrderWithoutPublishing(eventId);

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, ORDER_CREATED, Instant.now(), USER_ID, eventId, QUANTITY);
        rabbitTemplate.convertAndSend(EXCHANGE, ORDER_CREATED, event);
        rabbitTemplate.convertAndSend(EXCHANGE, ORDER_CREATED, event);

        awaitStatus(orderId, OrderStatus.COMPLETED);
        // İkinci mesaj geç işlense bile sold_count bir süre boyunca tek artışta kalmalı.
        await("aynı mesaj iki kez gelse de kontenjan bir kez ayrılmalı")
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(soldCount(eventId))
                        .as("sold_count yalnızca bir kez (sipariş adedi kadar) artmalı")
                        .isEqualTo(QUANTITY));

        assertThat(reservationCount(orderId)).as("sipariş için tek rezervasyon kaydı olmalı").isEqualTo(1);
        assertThat(ticketCount(orderId)).as("bilet sayısı iki katına çıkmamalı").isEqualTo(QUANTITY);
    }

    private Long saveOrderWithoutPublishing(Long eventId) {
        Instant now = Instant.now();
        Order order = new Order();
        order.setUserId(USER_ID);
        order.setUserEmail(USER_EMAIL);
        order.setEventId(eventId);
        order.setEventTitle("Saga Test");
        order.setQuantity(QUANTITY);
        order.setUnitPrice(new BigDecimal("100.00"));
        order.setTotalPrice(new BigDecimal("200.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return orderRepository.save(order).getId();
    }
}
