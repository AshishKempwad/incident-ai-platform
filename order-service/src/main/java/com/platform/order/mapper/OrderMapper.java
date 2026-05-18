package com.platform.order.mapper;
import com.platform.order.dto.*; import com.platform.order.entity.OrderEntity; import org.springframework.stereotype.Component;
@Component
public class OrderMapper {
    public OrderEntity toEntity(OrderRequestDto d){ OrderEntity e=new OrderEntity(); e.setUserId(d.userId()); e.setDescription(d.description()); e.setAmount(d.amount()); return e; }
    public OrderResponseDto toDto(OrderEntity e){ return new OrderResponseDto(e.getId(), e.getUserId(), e.getDescription(), e.getAmount()); }
}

