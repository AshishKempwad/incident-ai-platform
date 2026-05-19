package com.platform.payment.events;

public record NotificationTriggeredPayload(
        Long orderId,
        Long paymentId,
        String paymentStatus,
        String channel,
        String message
) {
}
