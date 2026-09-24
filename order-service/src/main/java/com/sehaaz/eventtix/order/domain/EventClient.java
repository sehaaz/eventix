package com.sehaaz.eventtix.order.domain;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.Instant;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/api/events/{id}")
    EventDetails getEvent(@PathVariable("id") Long id);

    record EventDetails(Long id, String title, BigDecimal price, Instant eventDate) {
    }
}
