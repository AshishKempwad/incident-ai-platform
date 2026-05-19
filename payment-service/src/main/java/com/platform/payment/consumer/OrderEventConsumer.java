package com.platform.payment.consumer;

import com.platform.payment.config.TopicNames;
import com.platform.payment.events.OrderCreatedEvent;
import com.platform.payment.service.PaymentEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final PaymentEventService paymentEventService;

    public OrderEventConsumer(PaymentEventService paymentEventService) {
        this.paymentEventService = paymentEventService;
    }

    @KafkaListener(topics = TopicNames.ORDER_CREATED, groupId = "${app.kafka.group-id}")
    public void consume(OrderCreatedEvent event, @Header("event-version") String version) {
        log.info("event=order_created_received eventId={} version={}", event.eventId(), version);
        paymentEventService.process(event);
    }
}
