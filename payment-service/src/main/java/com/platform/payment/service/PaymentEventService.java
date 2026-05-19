package com.platform.payment.service;

import com.platform.payment.entity.PaymentEntity;
import com.platform.payment.entity.ProcessedEventEntity;
import com.platform.payment.events.OrderCreatedEvent;
import com.platform.payment.producer.PaymentEventProducer;
import com.platform.payment.repository.PaymentRepository;
import com.platform.payment.repository.ProcessedEventRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentEventService {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventService.class);
    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentEventService(
            PaymentRepository paymentRepository,
            ProcessedEventRepository processedEventRepository,
            PaymentEventProducer paymentEventProducer) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public void process(OrderCreatedEvent event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            log.info("event=payment_idempotent_skip eventId={}", event.eventId());
            return;
        }

        String status = event.payload().amount().doubleValue() <= 1000.0 ? "COMPLETED" : "FAILED";
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(event.payload().orderId());
        payment.setPaymentMethod("AUTO");
        payment.setStatus(status);
        payment.setAmount(event.payload().amount());
        PaymentEntity saved = paymentRepository.save(payment);

        ProcessedEventEntity processed = new ProcessedEventEntity();
        processed.setEventId(event.eventId());
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);

        if ("COMPLETED".equals(status)) {
            paymentEventProducer.publishPaymentCompleted(saved, event.correlationId());
        } else {
            paymentEventProducer.publishPaymentFailed(saved, event.correlationId(), "amount_limit_exceeded");
        }
        paymentEventProducer.publishNotificationTriggered(saved, event.correlationId());
        log.info("event=payment_processed paymentId={} orderId={} status={}", saved.getId(), saved.getOrderId(), status);
    }
}
