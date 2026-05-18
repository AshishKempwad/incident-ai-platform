package com.platform.payment.dto;
import java.math.BigDecimal;
public record PaymentResponseDto(Long id, Long orderId, String paymentMethod, String status, BigDecimal amount) {}

