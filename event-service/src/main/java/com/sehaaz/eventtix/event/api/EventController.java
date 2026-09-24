package com.sehaaz.eventtix.event.api;

import com.sehaaz.eventtix.event.api.dto.EventRequest;
import com.sehaaz.eventtix.event.api.dto.EventResponse;
import com.sehaaz.eventtix.event.common.ApiException;
import com.sehaaz.eventtix.event.domain.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private static final String ROLE_HEADER = "X-User-Role";

    private final EventService eventService;

    @GetMapping
    public Page<EventResponse> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "eventDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return eventService.search(city, from, to, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@RequestHeader(value = ROLE_HEADER, required = false) String role,
                                @Valid @RequestBody EventRequest request) {
        requireAdmin(role);
        return eventService.create(request);
    }

    @PutMapping("/{id}")
    public EventResponse update(@RequestHeader(value = ROLE_HEADER, required = false) String role,
                                @PathVariable Long id, @Valid @RequestBody EventRequest request) {
        requireAdmin(role);
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = ROLE_HEADER, required = false) String role, @PathVariable Long id) {
        requireAdmin(role);
        eventService.delete(id);
    }

    private static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bu işlem için ADMIN yetkisi gerekli");
        }
    }
}
