package com.platform.payment.events;

import java.time.Instant;

public record PaymentFailedEvent(
        String eventId,
        String eventType,
        String eventVersion,
        Instant occurredAt,
        String source,
        String correlationId,
        PaymentResultPayload payload
) {
}
