package com.sehaaz.eventtix.order.saga;

import com.sehaaz.eventtix.order.domain.Order;
import com.sehaaz.eventtix.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaHappyPathTest extends SagaTestSupport {

    @Test
    void order_completes_andOneTicketIsGeneratedPerQuantity() {
        Long eventId = createEvent(10, 0);

        Long orderId = placeOrder(eventId, 3);
        Order order = awaitStatus(orderId, OrderStatus.COMPLETED);

        assertThat(order.getFailureReason()).as("tamamlanan siparişte hata sebebi olmamalı").isNull();
        assertThat(ticketCount(orderId)).as("üretilen bilet sayısı sipariş adedine eşit olmalı").isEqualTo(3);
        assertThat(soldCount(eventId)).as("etkinliğin sold_count değeri sipariş adedi kadar artmalı").isEqualTo(3);
    }
}
