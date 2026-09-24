package com.sehaaz.eventtix.order.domain;

import com.sehaaz.eventtix.common.event.OrderCancelledEvent;
import com.sehaaz.eventtix.common.event.OrderCompletedEvent;
import com.sehaaz.eventtix.common.event.OrderConfirmedEvent;
import com.sehaaz.eventtix.common.event.OrderCreatedEvent;
import com.sehaaz.eventtix.common.event.OrderFailedEvent;
import com.sehaaz.eventtix.order.api.dto.CreateOrderRequest;
import com.sehaaz.eventtix.order.api.dto.OrderResponse;
import com.sehaaz.eventtix.order.common.ApiException;
import com.sehaaz.eventtix.order.messaging.SagaPublisher;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_CANCELLED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_COMPLETED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_CONFIRMED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_CREATED;
import static com.sehaaz.eventtix.common.messaging.SagaMessaging.ORDER_FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventClient eventClient;
    private final SagaPublisher publisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public OrderResponse create(Long userId, CreateOrderRequest request) {
        // HTTP çağrısı transaction dışında: DB bağlantısı event-service'i beklerken tutulmasın.
        EventClient.EventDetails event = fetchEvent(request.eventId());
        Instant now = clock.instant();
        if (!event.eventDate().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_ACTIVE", "Etkinlik tarihi geçmiş");
        }

        return transactionTemplate.execute(status -> {
            Order order = new Order();
            order.setUserId(userId);
            order.setEventId(event.id());
            order.setEventTitle(event.title());
            order.setQuantity(request.quantity());
            order.setUnitPrice(event.price());
            order.setTotalPrice(event.price().multiply(BigDecimal.valueOf(request.quantity())));
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(now);
            order.setUpdatedAt(now);
            orderRepository.save(order);

            publisher.publishAfterCommit(ORDER_CREATED, new OrderCreatedEvent(
                    order.getId(), ORDER_CREATED, now, userId, order.getEventId(), order.getQuantity()));
            return OrderResponse.from(order);
        });
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findMine(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findMine(Long userId, Long orderId) {
        // Başkasının siparişi de 404: varlığı sızdırılmaz.
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Sipariş bulunamadı"));
    }

    @Transactional
    public void onQuotaReserved(Long orderId) {
        Order order = orderIn(orderId, OrderStatus.PENDING);
        if (order == null) {
            return;
        }
        order.setStatus(OrderStatus.QUOTA_RESERVED);
        order.setUpdatedAt(clock.instant());
        publisher.publishAfterCommit(ORDER_CONFIRMED, new OrderConfirmedEvent(
                orderId, ORDER_CONFIRMED, clock.instant(), order.getUserId(), order.getEventId(), order.getQuantity()));
    }

    @Transactional
    public void onQuotaRejected(Long orderId) {
        Order order = orderIn(orderId, OrderStatus.PENDING);
        if (order == null) {
            return;
        }
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(FailureReason.QUOTA);
        order.setUpdatedAt(clock.instant());
        publisher.publishAfterCommit(ORDER_FAILED, new OrderFailedEvent(
                orderId, ORDER_FAILED, clock.instant(), order.getUserId(), FailureReason.QUOTA.name()));
    }

    @Transactional
    public void onTicketGenerated(Long orderId) {
        Order order = orderIn(orderId, OrderStatus.QUOTA_RESERVED);
        if (order == null) {
            return;
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(clock.instant());
        publisher.publishAfterCommit(ORDER_COMPLETED, new OrderCompletedEvent(
                orderId, ORDER_COMPLETED, clock.instant(), order.getUserId(), order.getEventTitle(),
                order.getQuantity()));
    }

    @Transactional
    public void onTicketFailed(Long orderId) {
        Order order = orderIn(orderId, OrderStatus.QUOTA_RESERVED);
        if (order == null) {
            return;
        }
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(FailureReason.TICKET);
        order.setUpdatedAt(clock.instant());
        publisher.publishAfterCommit(ORDER_CANCELLED, new OrderCancelledEvent(
                orderId, ORDER_CANCELLED, clock.instant(), order.getUserId(), order.getEventId(),
                order.getQuantity(), FailureReason.TICKET.name()));
    }

    /**
     * Sadece beklenen durumdaki siparişler ilerler; tekrar gelen mesajda null döner (idempotency).
     */
    private Order orderIn(Long orderId, OrderStatus expected) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Sipariş bulunamadı: " + orderId));
        if (order.getStatus() != expected) {
            log.info("Sipariş {} zaten {} durumunda, mesaj atlandı", orderId, order.getStatus());
            return null;
        }
        return order;
    }

    private EventClient.EventDetails fetchEvent(Long eventId) {
        try {
            return eventClient.getEvent(eventId);
        } catch (FeignException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Etkinlik bulunamadı");
        } catch (FeignException e) {
            log.error("event-service çağrısı başarısız", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EVENT_SERVICE_UNAVAILABLE",
                    "Etkinlik bilgisi alınamadı");
        }
    }
}
