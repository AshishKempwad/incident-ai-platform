package com.platform.payment.controller;
import com.platform.payment.dto.*; import com.platform.payment.service.PaymentService; import jakarta.validation.Valid; import java.util.List; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service; public PaymentController(PaymentService service){ this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public PaymentResponseDto create(@Valid @RequestBody PaymentRequestDto dto){ return service.create(dto); }
    @GetMapping public List<PaymentResponseDto> list(){ return service.list(); }
    @GetMapping("/{id}") public PaymentResponseDto getById(@PathVariable Long id){ return service.getById(id); }
}

