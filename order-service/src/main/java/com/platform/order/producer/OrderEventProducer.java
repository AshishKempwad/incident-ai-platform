package com.platform.order.producer;

import com.platform.order.config.TopicNames;
import com.platform.order.events.OrderCreatedEvent;
import com.platform.order.events.OrderCreatedPayload;
import com.platform.order.entity.OrderEntity;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String EVENT_VERSION = "v1";
    private static final String EVENT_TYPE = "OrderCreated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderEntity orderEntity) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                "order-service",
                MDC.get("correlationId"),
                new OrderCreatedPayload(
                        orderEntity.getId(),
                        orderEntity.getUserId(),
                        orderEntity.getDescription(),
                        orderEntity.getAmount()
                )
        );

        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TopicNames.ORDER_CREATED)
                .setHeader(KafkaHeaders.KEY, String.valueOf(orderEntity.getId()))
                .setHeader("event-version", EVENT_VERSION)
                .build();

        kafkaTemplate.send(message);
        log.info("event=order_created_published orderId={} eventId={}", orderEntity.getId(), event.eventId());
    }
}
