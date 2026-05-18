package com.platform.order.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record OrderRequestDto(
        @NotNull Long userId,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}

