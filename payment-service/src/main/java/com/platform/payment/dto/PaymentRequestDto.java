package com.platform.payment.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record PaymentRequestDto(
        @NotNull Long orderId,
        @NotBlank String paymentMethod,
        @NotBlank String status,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}

