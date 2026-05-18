package com.platform.payment.mapper;
import com.platform.payment.dto.*; import com.platform.payment.entity.PaymentEntity; import org.springframework.stereotype.Component;
@Component
public class PaymentMapper {
    public PaymentEntity toEntity(PaymentRequestDto d){ PaymentEntity e=new PaymentEntity(); e.setOrderId(d.orderId()); e.setPaymentMethod(d.paymentMethod()); e.setStatus(d.status()); e.setAmount(d.amount()); return e; }
    public PaymentResponseDto toDto(PaymentEntity e){ return new PaymentResponseDto(e.getId(), e.getOrderId(), e.getPaymentMethod(), e.getStatus(), e.getAmount()); }
}

