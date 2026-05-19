package com.platform.order.events;

import java.math.BigDecimal;

public record OrderCreatedPayload(
        Long orderId,
        Long userId,
        String description,
        BigDecimal amount
) {
}
