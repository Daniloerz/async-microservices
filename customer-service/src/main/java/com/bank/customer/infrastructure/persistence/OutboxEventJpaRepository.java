package com.bank.customer.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findByPublishedAtIsNullOrderByIdAsc(Limit limit);

    @Transactional
    @Modifying
    @Query(value = "UPDATE outbox_event SET published_at = now() WHERE id = :id", nativeQuery = true)
    int markPublished(@Param("id") Long id);
}
