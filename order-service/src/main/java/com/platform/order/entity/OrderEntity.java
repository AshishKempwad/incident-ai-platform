package com.platform.order.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 255) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal amount) { this.amount = amount; }
}

