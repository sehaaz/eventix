package com.sehaaz.eventtix.order.api;

import com.sehaaz.eventtix.order.api.dto.CreateOrderRequest;
import com.sehaaz.eventtix.order.api.dto.OrderResponse;
import com.sehaaz.eventtix.order.domain.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestHeader(USER_ID_HEADER) Long userId,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(userId, request);
    }

    @GetMapping("/me")
    public List<OrderResponse> mine(@RequestHeader(USER_ID_HEADER) Long userId) {
        return orderService.findMine(userId);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        return orderService.findMine(userId, id);
    }
}
