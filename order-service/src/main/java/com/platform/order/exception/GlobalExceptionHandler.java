package com.platform.order.exception;
import jakarta.servlet.http.HttpServletRequest; import java.time.Instant; import java.util.Map; import java.util.stream.Collectors;
import org.springframework.http.*; import org.springframework.validation.FieldError; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class) public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req){ return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), Map.of()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req){ Map<String,String> f=ex.getBindingResult().getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,(a,b)->a)); return ResponseEntity.badRequest().body(err(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), f));}
    @ExceptionHandler(Exception.class) public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req){ return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req.getRequestURI(), Map.of()));}
    private ApiError err(HttpStatus s, String m, String p, Map<String,String> fe){ return new ApiError(Instant.now(), s.value(), s.getReasonPhrase(), m, p, fe); }
}

