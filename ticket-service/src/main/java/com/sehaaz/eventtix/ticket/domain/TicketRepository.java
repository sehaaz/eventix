package com.sehaaz.eventtix.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrderIdOrderBySeqNo(Long orderId);

    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Ticket> findByTicketCodeAndUserId(String ticketCode, Long userId);
}
