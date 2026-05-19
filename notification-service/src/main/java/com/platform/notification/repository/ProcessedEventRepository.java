package com.platform.notification.repository;

import com.platform.notification.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {
    boolean existsByEventId(String eventId);
}
