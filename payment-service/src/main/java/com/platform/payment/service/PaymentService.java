package com.platform.payment.service;
import com.platform.payment.dto.*; import com.platform.payment.entity.PaymentEntity; import com.platform.payment.exception.ResourceNotFoundException; import com.platform.payment.mapper.PaymentMapper; import com.platform.payment.repository.PaymentRepository;
import java.util.List; import org.slf4j.*; import org.springframework.stereotype.Service;
@Service
public class PaymentService {
    private static final Logger log= LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository repository; private final PaymentMapper mapper;
    public PaymentService(PaymentRepository repository, PaymentMapper mapper){ this.repository=repository; this.mapper=mapper; }
    public PaymentResponseDto create(PaymentRequestDto dto){ PaymentEntity saved=repository.save(mapper.toEntity(dto)); log.info("event=payment_created paymentId={}", saved.getId()); return mapper.toDto(saved); }
    public List<PaymentResponseDto> list(){ return repository.findAll().stream().map(mapper::toDto).toList(); }
    public PaymentResponseDto getById(Long id){ return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("payment not found: "+id))); }
}

