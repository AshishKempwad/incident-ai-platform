package com.platform.payment.repository;

import com.platform.payment.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {
    boolean existsByEventId(String eventId);
}
