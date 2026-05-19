package com.platform.payment.events;

import java.math.BigDecimal;

public record PaymentResultPayload(
        Long paymentId,
        Long orderId,
        String status,
        BigDecimal amount,
        String reason
) {
}
