package com.platform.order.service;

import com.platform.order.dto.OrderRequestDto;
import com.platform.order.dto.OrderResponseDto;
import com.platform.order.entity.OrderEntity;
import com.platform.order.exception.ResourceNotFoundException;
import com.platform.order.mapper.OrderMapper;
import com.platform.order.producer.OrderEventProducer;
import com.platform.order.repository.OrderRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderRepository repository, OrderMapper mapper, OrderEventProducer orderEventProducer) {
        this.repository = repository;
        this.mapper = mapper;
        this.orderEventProducer = orderEventProducer;
    }

    @Transactional
    public OrderResponseDto create(OrderRequestDto dto) {
        OrderEntity saved = repository.save(mapper.toEntity(dto));
        orderEventProducer.publishOrderCreated(saved);
        log.info("event=order_created orderId={}", saved.getId());
        return mapper.toDto(saved);
    }

    public List<OrderResponseDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public OrderResponseDto getById(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order not found: " + id)));
    }
}
