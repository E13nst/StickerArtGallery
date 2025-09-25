package com.example.sticker_art_gallery.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик ошибок валидации
 */
@RestControllerAdvice
public class ValidationExceptionHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationExceptionHandler.class);
    
    /**
     * Обработка ошибок валидации @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        LOGGER.warn("❌ Ошибка валидации: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
            LOGGER.warn("  - Поле '{}': {}", fieldName, errorMessage);
        });
        
        response.put("error", "Ошибка валидации");
        response.put("message", "Некорректные данные в запросе");
        response.put("validationErrors", errors);
        response.put("timestamp", java.time.OffsetDateTime.now());
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Обработка ошибок валидации @Validated
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            jakarta.validation.ConstraintViolationException ex) {
        LOGGER.warn("❌ Ошибка валидации ограничений: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
            LOGGER.warn("  - Поле '{}': {}", fieldName, errorMessage);
        });
        
        response.put("error", "Ошибка валидации");
        response.put("message", "Нарушены ограничения данных");
        response.put("validationErrors", errors);
        response.put("timestamp", java.time.OffsetDateTime.now());
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Обработка общих ошибок валидации
     */
    @ExceptionHandler(jakarta.validation.ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            jakarta.validation.ValidationException ex) {
        LOGGER.warn("❌ Общая ошибка валидации: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Ошибка валидации");
        response.put("message", ex.getMessage());
        response.put("timestamp", java.time.OffsetDateTime.now());
        
        return ResponseEntity.badRequest().body(response);
    }
}
