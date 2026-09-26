package com.sehaaz.eventtix.order.saga;

import com.sehaaz.eventtix.order.domain.FailureReason;
import com.sehaaz.eventtix.order.domain.Order;
import com.sehaaz.eventtix.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaQuotaRejectedTest extends SagaTestSupport {

    @Test
    void orderForSoldOutEvent_failsWithQuotaReason() {
        Long eventId = createEvent(5, 5);

        Long orderId = placeOrder(eventId, 1);
        Order order = awaitStatus(orderId, OrderStatus.FAILED);

        assertThat(order.getFailureReason()).as("kontenjan dolu olduğu için sebep QUOTA olmalı")
                .isEqualTo(FailureReason.QUOTA);
        assertThat(soldCount(eventId)).as("dolu etkinliğin sold_count değeri değişmemeli").isEqualTo(5);
        assertThat(reservationCount(orderId)).as("reddedilen sipariş için rezervasyon kaydı olmamalı").isZero();
        assertThat(ticketCount(orderId)).as("reddedilen sipariş için bilet üretilmemeli").isZero();
    }
}
