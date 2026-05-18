package com.platform.order.dto;
import java.math.BigDecimal;
public record OrderResponseDto(Long id, Long userId, String description, BigDecimal amount) {}

