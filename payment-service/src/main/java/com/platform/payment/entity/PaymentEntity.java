package com.platform.payment.entity;
import jakarta.persistence.*; import java.math.BigDecimal;
@Entity @Table(name = "payments")
public class PaymentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long orderId;
    @Column(nullable = false, length = 40) private String paymentMethod;
    @Column(nullable = false, length = 40) private String status;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getOrderId(){return orderId;} public void setOrderId(Long orderId){this.orderId=orderId;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String paymentMethod){this.paymentMethod=paymentMethod;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal amount){this.amount=amount;}
}

