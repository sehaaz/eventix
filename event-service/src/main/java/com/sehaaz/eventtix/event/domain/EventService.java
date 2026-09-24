package com.sehaaz.eventtix.event.domain;

import com.sehaaz.eventtix.event.api.dto.EventRequest;
import com.sehaaz.eventtix.event.api.dto.EventResponse;
import com.sehaaz.eventtix.event.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final QuotaReservationRepository reservationRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<EventResponse> search(String city, Instant from, Instant to, Pageable pageable) {
        return eventRepository.findAll(EventSpecifications.filter(city, from, to), pageable)
                .map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) {
        return EventResponse.from(find(id));
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        Event event = new Event();
        apply(event, request);
        event.setCreatedAt(clock.instant());
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        Event event = find(id);
        if (request.totalQuota() < event.getSoldCount()) {
            throw new ApiException(HttpStatus.CONFLICT, "QUOTA_BELOW_SOLD",
                    "Kontenjan satılan bilet sayısının altına düşürülemez");
        }
        apply(event, request);
        return EventResponse.from(event);
    }

    @Transactional
    public void delete(Long id) {
        eventRepository.delete(find(id));
    }

    /**
     * Kontenjan ayırır. Aynı orderId için tekrar çağrılırsa (tekrar gelen mesaj) yeniden ayırmaz.
     */
    @Transactional
    public boolean reserveQuota(Long orderId, Long eventId, int qty) {
        if (reservationRepository.existsById(orderId)) {
            return true;
        }
        if (eventRepository.reserve(eventId, qty) == 0) {
            return false;
        }
        reservationRepository.save(new QuotaReservation(orderId, eventId, qty, clock.instant()));
        return true;
    }

    @Transactional
    public void releaseQuota(Long orderId) {
        reservationRepository.findForUpdate(orderId).ifPresent(reservation -> {
            eventRepository.release(reservation.getEventId(), reservation.getQuantity());
            reservationRepository.delete(reservation);
        });
    }

    private Event find(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Etkinlik bulunamadı"));
    }

    private static void apply(Event event, EventRequest request) {
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setCity(request.city());
        event.setEventDate(request.eventDate());
        event.setPrice(request.price());
        event.setTotalQuota(request.totalQuota());
        event.setImageUrl(request.imageUrl());
    }
}
