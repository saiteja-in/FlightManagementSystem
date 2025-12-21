package com.saiteja.apigateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationExceptions(WebExchangeBindException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String field = error.getField();
            String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
            errors.put(field, message);
        });

        response.put("message", "Validation failed");
        response.put("errors", errors);
        
        // Single error message for backward compatibility
        String allErrors = errors.values().stream().collect(Collectors.joining("; "));
        response.put("error", allErrors);

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }
}

