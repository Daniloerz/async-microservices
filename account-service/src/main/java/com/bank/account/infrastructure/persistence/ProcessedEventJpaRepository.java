package com.bank.account.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    @Modifying
    @Query(value = "INSERT INTO processed_event (event_id) VALUES (:eventId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId);
}
