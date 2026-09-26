package com.sehaaz.eventtix.order.saga;

import com.sehaaz.eventtix.order.domain.FailureReason;
import com.sehaaz.eventtix.order.domain.Order;
import com.sehaaz.eventtix.order.domain.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
class SagaCompensationTest extends SagaTestSupport {

    // ticket-service bilet dosyalarını bu dizine yazar (application.yml: ticket.storage-dir).
    private static final String TICKET_STORAGE_DIR = "/app/tickets";

    // Dizinin yerine düz dosya koyulunca Files.createDirectories IOException fırlatır → ticket.failed.
    @BeforeEach
    void breakTicketStorage() throws Exception {
        exec("rm -rf " + TICKET_STORAGE_DIR + " && touch " + TICKET_STORAGE_DIR);
    }

    @AfterEach
    void restoreTicketStorage() throws Exception {
        exec("rm -f " + TICKET_STORAGE_DIR);
    }

    @Test
    void ticketGenerationFailure_failsOrder_andReleasesReservedQuota() {
        Long eventId = createEvent(10, 4);
        int soldBefore = soldCount(eventId);
        log.info("[SAGA] Sipariş öncesi sold_count = {}", soldBefore);

        Long orderId = placeOrder(eventId, 2);
        Order order = awaitStatus(orderId, OrderStatus.FAILED);
        log.info("[SAGA] Sipariş {} durumu = {}, sebep = {}", orderId, order.getStatus(), order.getFailureReason());

        assertThat(order.getFailureReason()).as("bilet üretimi başarısız olduğu için sebep TICKET olmalı")
                .isEqualTo(FailureReason.TICKET);
        await("event-service kontenjanı geri vermeli")
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(soldCount(eventId))
                        .as("telafi sonrası sold_count sipariş öncesi değerine dönmeli")
                        .isEqualTo(soldBefore));
        log.info("[SAGA] Telafi sonrası sold_count = {} (kontenjan geri verildi)", soldCount(eventId));

        assertThat(reservationCount(orderId)).as("telafi sonrası rezervasyon kaydı silinmeli").isZero();
        assertThat(ticketCount(orderId)).as("başarısız siparişte hiç bilet kalmamalı").isZero();
    }

    private static void exec(String command) throws Exception {
        assertThat(TICKET_SERVICE.execInContainer("sh", "-c", command).getExitCode())
                .as("ticket-service container komutu başarılı olmalı: " + command)
                .isZero();
    }
}
