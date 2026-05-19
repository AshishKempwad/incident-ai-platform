package com.platform.notification.service;

import com.platform.notification.entity.NotificationEntity;
import com.platform.notification.entity.ProcessedEventEntity;
import com.platform.notification.events.NotificationTriggeredEvent;
import com.platform.notification.repository.NotificationRepository;
import com.platform.notification.repository.ProcessedEventRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventService {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventService.class);
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    public NotificationEventService(
            NotificationRepository notificationRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void process(NotificationTriggeredEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("event=notification_idempotent_skip eventId={}", event.eventId());
            return;
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setOrderId(event.payload().orderId());
        notification.setPaymentId(event.payload().paymentId());
        notification.setPaymentStatus(event.payload().paymentStatus());
        notification.setChannel(event.payload().channel());
        notification.setMessage(event.payload().message());
        notification.setCreatedAt(Instant.now());
        NotificationEntity saved = notificationRepository.save(notification);

        ProcessedEventEntity processed = new ProcessedEventEntity();
        processed.setEventId(event.eventId());
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);

        log.info("event=notification_persisted notificationId={} orderId={}", saved.getId(), saved.getOrderId());
    }
}
