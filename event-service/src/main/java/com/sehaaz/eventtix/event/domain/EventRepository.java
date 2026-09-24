package com.sehaaz.eventtix.event.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // Tek koşullu UPDATE: satır kilidi sayesinde eşzamanlı isteklerde kontenjan aşılamaz.
    @Modifying
    @Query("UPDATE Event e SET e.soldCount = e.soldCount + :qty "
            + "WHERE e.id = :id AND e.soldCount + :qty <= e.totalQuota")
    int reserve(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Event e SET e.soldCount = e.soldCount - :qty WHERE e.id = :id")
    int release(@Param("id") Long id, @Param("qty") int qty);
}
