package com.platform.payment.producer;

import com.platform.payment.config.TopicNames;
import com.platform.payment.entity.PaymentEntity;
import com.platform.payment.events.NotificationTriggeredEvent;
import com.platform.payment.events.NotificationTriggeredPayload;
import com.platform.payment.events.PaymentCompletedEvent;
import com.platform.payment.events.PaymentFailedEvent;
import com.platform.payment.events.PaymentResultPayload;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String EVENT_VERSION = "v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentCompleted(PaymentEntity paymentEntity, String correlationId) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                "PaymentCompleted",
                EVENT_VERSION,
                Instant.now(),
                "payment-service",
                correlationId,
                new PaymentResultPayload(
                        paymentEntity.getId(),
                        paymentEntity.getOrderId(),
                        paymentEntity.getStatus(),
                        paymentEntity.getAmount(),
                        null
                )
        );
        send(TopicNames.PAYMENT_COMPLETED, String.valueOf(paymentEntity.getOrderId()), event);
        log.info("event=payment_completed_published paymentId={} eventId={}", paymentEntity.getId(), event.eventId());
    }

    public void publishPaymentFailed(PaymentEntity paymentEntity, String correlationId, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID().toString(),
                "PaymentFailed",
                EVENT_VERSION,
                Instant.now(),
                "payment-service",
                correlationId,
                new PaymentResultPayload(
                        paymentEntity.getId(),
                        paymentEntity.getOrderId(),
                        paymentEntity.getStatus(),
                        paymentEntity.getAmount(),
                        reason
                )
        );
        send(TopicNames.PAYMENT_FAILED, String.valueOf(paymentEntity.getOrderId()), event);
        log.info("event=payment_failed_published paymentId={} eventId={}", paymentEntity.getId(), event.eventId());
    }

    public void publishNotificationTriggered(PaymentEntity paymentEntity, String correlationId) {
        String message = "Payment status for order " + paymentEntity.getOrderId() + " is " + paymentEntity.getStatus();
        NotificationTriggeredEvent event = new NotificationTriggeredEvent(
                UUID.randomUUID().toString(),
                "NotificationTriggered",
                EVENT_VERSION,
                Instant.now(),
                "payment-service",
                correlationId,
                new NotificationTriggeredPayload(
                        paymentEntity.getOrderId(),
                        paymentEntity.getId(),
                        paymentEntity.getStatus(),
                        "EMAIL",
                        message
                )
        );
        send(TopicNames.NOTIFICATION_TRIGGERED, String.valueOf(paymentEntity.getOrderId()), event);
        log.info("event=notification_triggered_published paymentId={} eventId={}", paymentEntity.getId(), event.eventId());
    }

    private void send(String topic, String key, Object payload) {
        Message<Object> message = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader("event-version", EVENT_VERSION)
                .build();
        kafkaTemplate.send(message);
    }
}
