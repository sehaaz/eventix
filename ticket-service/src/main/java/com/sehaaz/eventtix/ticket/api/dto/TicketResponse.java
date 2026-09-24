package com.sehaaz.eventtix.ticket.api.dto;

import com.sehaaz.eventtix.ticket.domain.Ticket;

import java.time.Instant;

public record TicketResponse(
        String ticketCode,
        Long orderId,
        Long eventId,
        boolean used,
        Instant createdAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getTicketCode(), ticket.getOrderId(), ticket.getEventId(),
                ticket.isUsed(), ticket.getCreatedAt());
    }
}
