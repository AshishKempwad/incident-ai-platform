package com.platform.order.service;
import com.platform.order.dto.*; import com.platform.order.entity.OrderEntity; import com.platform.order.exception.ResourceNotFoundException; import com.platform.order.mapper.OrderMapper; import com.platform.order.repository.OrderRepository;
import java.util.List; import org.slf4j.*; import org.springframework.stereotype.Service;
@Service
public class OrderService {
    private static final Logger log= LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repository;
    private final OrderMapper mapper;

    public OrderService(OrderRepository repository, OrderMapper mapper){
        this.repository=repository; this.mapper=mapper;
    }

    public OrderResponseDto create(OrderRequestDto dto){
        OrderEntity saved=repository.save(mapper.toEntity(dto)); log.info("event=order_created orderId={}", saved.getId()); return mapper.toDto(saved);
    }

    public List<OrderResponseDto> list(){
        return repository.findAll().stream().map(mapper::toDto).toList();
    }
    public OrderResponseDto getById(Long id){
        return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("order not found: "+id)));
    }
}

