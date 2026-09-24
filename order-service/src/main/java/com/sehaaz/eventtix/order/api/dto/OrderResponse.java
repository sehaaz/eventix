package com.sehaaz.eventtix.order.api.dto;

import com.sehaaz.eventtix.order.domain.FailureReason;
import com.sehaaz.eventtix.order.domain.Order;
import com.sehaaz.eventtix.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long eventId,
        String eventTitle,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        OrderStatus status,
        FailureReason failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getEventId(), order.getEventTitle(), order.getQuantity(),
                order.getUnitPrice(), order.getTotalPrice(), order.getStatus(), order.getFailureReason(),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
