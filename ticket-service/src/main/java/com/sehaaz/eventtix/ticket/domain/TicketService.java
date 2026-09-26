package com.sehaaz.eventtix.ticket.domain;

import com.google.zxing.WriterException;
import com.sehaaz.eventtix.ticket.api.dto.TicketResponse;
import com.sehaaz.eventtix.ticket.common.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketFileWriter fileWriter;
    private final Clock clock;

    /**
     * Sipariş için quantity kadar bilet üretir ve kodlarını döner.
     * Aynı orderId için tekrar çağrılırsa (tekrar gelen mesaj) yeniden üretmez, mevcut kodları döner.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> generate(Long orderId, Long userId, Long eventId, int quantity)
            throws IOException, WriterException {
        List<Ticket> existing = ticketRepository.findByOrderIdOrderBySeqNo(orderId);
        if (!existing.isEmpty()) {
            log.info("Sipariş {} için biletler zaten üretilmiş, tekrar üretilmedi", orderId);
            return existing.stream().map(Ticket::getTicketCode).toList();
        }

        Instant now = clock.instant();
        List<String> codes = new ArrayList<>();
        for (int seqNo = 1; seqNo <= quantity; seqNo++) {
            Ticket ticket = new Ticket();
            ticket.setOrderId(orderId);
            ticket.setSeqNo(seqNo);
            ticket.setUserId(userId);
            ticket.setEventId(eventId);
            ticket.setTicketCode(UUID.randomUUID().toString());
            ticket.setCreatedAt(now);

            Path qrPath = fileWriter.writeQr(ticket.getTicketCode());
            ticket.setQrPath(qrPath.toString());
            ticket.setPdfPath(fileWriter.writePdf(ticket, quantity, qrPath).toString());
            ticketRepository.save(ticket);
            codes.add(ticket.getTicketCode());
        }
        return codes;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findMine(Long userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Path pdfOf(Long userId, String ticketCode) {
        // Başkasının bileti de 404: varlığı sızdırılmaz.
        return ticketRepository.findByTicketCodeAndUserId(ticketCode, userId)
                .map(ticket -> Path.of(ticket.getPdfPath()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Bilet bulunamadı"));
    }

    @Transactional(readOnly = true)
    public Path qrOf(Long userId, String ticketCode) {
        return ticketRepository.findByTicketCodeAndUserId(ticketCode, userId)
                .map(ticket -> Path.of(ticket.getQrPath()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Bilet bulunamadı"));
    }
}
