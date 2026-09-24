package com.sehaaz.eventtix.event.domain;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> filter(String city, Instant from, Instant to) {
        return (root, query, cb) -> cb.and(
                city == null ? cb.conjunction() : cb.equal(cb.lower(root.get("city")), city.toLowerCase()),
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("eventDate"), from),
                to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("eventDate"), to));
    }
}
