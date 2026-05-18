package com.platform.order.repository;
import com.platform.order.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {}

