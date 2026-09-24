package com.sehaaz.eventtix.ticket.api;

import com.sehaaz.eventtix.ticket.api.dto.TicketResponse;
import com.sehaaz.eventtix.ticket.domain.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final TicketService ticketService;

    @GetMapping("/me")
    public List<TicketResponse> mine(@RequestHeader(USER_ID_HEADER) Long userId) {
        return ticketService.findMine(userId);
    }

    @GetMapping("/{code}/pdf")
    public ResponseEntity<Resource> pdf(@RequestHeader(USER_ID_HEADER) Long userId, @PathVariable String code) {
        Resource pdf = new FileSystemResource(ticketService.pdfOf(userId, code));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(code + ".pdf").build().toString())
                .body(pdf);
    }
}
