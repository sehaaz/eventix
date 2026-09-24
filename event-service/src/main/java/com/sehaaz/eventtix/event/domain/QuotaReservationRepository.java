package com.sehaaz.eventtix.event.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuotaReservationRepository extends JpaRepository<QuotaReservation, Long> {

    // SELECT ... FOR UPDATE: aynı sipariş için eşzamanlı iki iade kontenjanı iki kez geri vermesin.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM QuotaReservation r WHERE r.orderId = :orderId")
    Optional<QuotaReservation> findForUpdate(@Param("orderId") Long orderId);
}
