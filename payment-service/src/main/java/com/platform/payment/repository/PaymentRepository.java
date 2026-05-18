package com.platform.payment.repository;
import com.platform.payment.entity.PaymentEntity; import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {}

