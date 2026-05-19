package com.platform.notification.consumer;

import com.platform.notification.config.TopicNames;
import com.platform.notification.events.NotificationTriggeredEvent;
import com.platform.notification.service.NotificationEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);
    private final NotificationEventService notificationEventService;

    public NotificationEventConsumer(NotificationEventService notificationEventService) {
        this.notificationEventService = notificationEventService;
    }

    @KafkaListener(topics = TopicNames.NOTIFICATION_TRIGGERED, groupId = "${app.kafka.group-id}")
    public void consume(NotificationTriggeredEvent event) {
        if (event == null) {
            log.warn("event=notification_triggered_null_payload");
            return;
        }
        log.info("event=notification_triggered_received eventId={} version={}", event.eventId(), event.eventVersion());
        notificationEventService.process(event);
    }
}
