package com.sehaaz.eventtix.event.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
@Testcontainers
class EventQuotaConcurrencyTest {

    private static final int THREADS = 10;
    private static final int QUOTA = 5;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EventService eventService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private QuotaReservationRepository reservationRepository;

    @Test
    void parallelReservations_neverExceedQuota() throws Exception {
        Long eventId = eventRepository.save(event(QUOTA)).getId();

        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (long orderId = 1_000; orderId < 1_000 + THREADS; orderId++) {
            long id = orderId;
            tasks.add(() -> {
                start.await();
                return eventService.reserveQuota(id, eventId, 1);
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Boolean>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();

            int succeeded = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    succeeded++;
                }
            }

            assertThat(succeeded).isEqualTo(QUOTA);
            assertThat(eventRepository.findById(eventId).orElseThrow().getSoldCount()).isEqualTo(QUOTA);
            assertThat(reservationRepository.findAll())
                    .filteredOn(r -> r.getEventId().equals(eventId))
                    .hasSize(QUOTA);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Event event(int quota) {
        Event event = new Event();
        event.setTitle("Test");
        event.setVenue("Salon");
        event.setCity("İstanbul");
        event.setEventDate(Instant.parse("2027-01-01T20:00:00Z"));
        event.setPrice(new BigDecimal("100.00"));
        event.setTotalQuota(quota);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
