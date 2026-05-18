package com.platform.order.controller;
import com.platform.order.dto.*; import com.platform.order.service.OrderService; import jakarta.validation.Valid; import java.util.List; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service; public OrderController(OrderService service){ this.service=service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public OrderResponseDto create(@Valid @RequestBody OrderRequestDto dto){ return service.create(dto);}
    @GetMapping public List<OrderResponseDto> list(){ return service.list(); }
    @GetMapping("/{id}") public OrderResponseDto getById(@PathVariable Long id){ return service.getById(id); }
}

