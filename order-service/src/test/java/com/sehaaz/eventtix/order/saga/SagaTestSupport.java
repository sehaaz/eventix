package com.sehaaz.eventtix.order.saga;

import com.sehaaz.eventtix.order.api.dto.CreateOrderRequest;
import com.sehaaz.eventtix.order.domain.Order;
import com.sehaaz.eventtix.order.domain.OrderRepository;
import com.sehaaz.eventtix.order.domain.OrderService;
import com.sehaaz.eventtix.order.domain.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * Uçtan uca saga ortamı: PostgreSQL + RabbitMQ + gerçek event-service ve ticket-service container'ları,
 * order-service ise test JVM'inde çalışır. Servis image'ları docker compose'un build ettikleridir:
 * testten önce `docker compose build event ticket` koşulmalı, yoksa eski kodla çalışır.
 * Container'lar tüm test sınıfları için bir kez başlar.
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
abstract class SagaTestSupport {

    protected static final long USER_ID = 42L;
    protected static final String USER_EMAIL = "saga@test.com";

    private static final Network NETWORK = Network.newNetwork();

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres")
            .withStartupTimeout(Duration.ofMinutes(3));

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3-management-alpine")
            .withNetwork(NETWORK)
            .withNetworkAliases("rabbitmq")
            .withStartupTimeout(Duration.ofMinutes(5));

    static final GenericContainer<?> EVENT_SERVICE = service("projecteventix-event", 8082);
    static final GenericContainer<?> TICKET_SERVICE = service("projecteventix-ticket", 8084);

    static {
        Startables.deepStart(POSTGRES, RABBITMQ).join();
        Startables.deepStart(EVENT_SERVICE, TICKET_SERVICE).join();
    }

    @Autowired
    protected OrderService orderService;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void eventServiceUri(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.event-service[0].uri",
                () -> "http://" + EVENT_SERVICE.getHost() + ":" + EVENT_SERVICE.getMappedPort(8082));
    }

    protected Long createEvent(int totalQuota, int soldCount) {
        return jdbc.queryForObject("""
                INSERT INTO event_schema.events (title, venue, city, event_date, price, total_quota, sold_count, created_at)
                VALUES ('Saga Test', 'Salon', 'İstanbul', now() + interval '30 days', 100.00, ?, ?, now())
                RETURNING id
                """, Long.class, totalQuota, soldCount);
    }

    protected int soldCount(Long eventId) {
        return jdbc.queryForObject("SELECT sold_count FROM event_schema.events WHERE id = ?", Integer.class, eventId);
    }

    protected int ticketCount(Long orderId) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket_schema.tickets WHERE order_id = ?", Integer.class, orderId);
    }

    protected int reservationCount(Long orderId) {
        return jdbc.queryForObject("SELECT count(*) FROM event_schema.quota_reservations WHERE order_id = ?",
                Integer.class, orderId);
    }

    protected Long placeOrder(Long eventId, int quantity) {
        return orderService.create(USER_ID, USER_EMAIL, new CreateOrderRequest(eventId, quantity)).id();
    }

    protected Order awaitStatus(Long orderId, OrderStatus expected) {
        return await("sipariş " + orderId + " durumu " + expected + " olmalı")
                .atMost(Duration.ofSeconds(60))
                .until(() -> orderRepository.findById(orderId).orElseThrow(), order -> order.getStatus() == expected);
    }

    private static GenericContainer<?> service(String image, int port) {
        return new GenericContainer<>(image)
                .withNetwork(NETWORK)
                .withEnv("DB_URL", "jdbc:postgresql://postgres:5432/" + POSTGRES.getDatabaseName())
                .withEnv("DB_USER", POSTGRES.getUsername())
                .withEnv("DB_PASSWORD", POSTGRES.getPassword())
                .withEnv("RABBITMQ_HOST", "rabbitmq")
                .withEnv("RABBITMQ_USER", RABBITMQ.getAdminUsername())
                .withEnv("RABBITMQ_PASSWORD", RABBITMQ.getAdminPassword())
                .withEnv("EUREKA_CLIENT_ENABLED", "false")
                .withExposedPorts(port)
                .waitingFor(Wait.forLogMessage(".*Started .*ServiceApplication.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(5)));
    }
}
