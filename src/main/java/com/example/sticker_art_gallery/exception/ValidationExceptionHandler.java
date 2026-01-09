package com.example.sticker_art_gallery.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    
    /**
     * Обработка ошибок парсинга JSON (некорректное тело запроса)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParseError(HttpMessageNotReadableException ex) {
        LOGGER.warn("❌ Некорректный JSON в запросе: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", "Некорректный JSON в запросе");
        response.put("timestamp", java.time.OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Обработка ошибок авторизации Spring Security (401/403)
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        // Определяем, аутентифицирован ли пользователь
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null 
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !"anonymousUser".equals(authentication.getName());
        
        HttpStatus status;
        String errorMessage;
        
        if (!isAuthenticated) {
            // Пользователь не аутентифицирован - 401 Unauthorized
            status = HttpStatus.UNAUTHORIZED;
            errorMessage = "Требуется авторизация. Пожалуйста, обновите данные авторизации Telegram Web App (initData может быть устаревшим)";
            LOGGER.warn("⚠️ Доступ запрещен: пользователь не аутентифицирован. {}", ex.getMessage());
        } else {
            // Пользователь аутентифицирован, но нет прав - 403 Forbidden
            status = HttpStatus.FORBIDDEN;
            errorMessage = "Доступ запрещен. У вас нет прав для выполнения этого действия";
            String username = authentication != null ? authentication.getName() : "unknown";
            LOGGER.warn("⚠️ Доступ запрещен для пользователя {}: недостаточно прав. {}", 
                    username, ex.getMessage());
        }
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", status == HttpStatus.UNAUTHORIZED ? "Unauthorized" : "Forbidden");
        body.put("message", errorMessage);
        body.put("timestamp", java.time.OffsetDateTime.now());
        
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Обработка StickerSetNotFoundException (404)
     */
    @ExceptionHandler(StickerSetNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleStickerSetNotFound(StickerSetNotFoundException e) {
        LOGGER.warn("⚠️ StickerSet не найден: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", e.getMessage()));
    }

    /**
     * Обработка WalletNotFoundException (400)
     */
    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWalletNotFound(WalletNotFoundException e) {
        LOGGER.warn("⚠️ Кошелёк не найден: {}", e.getMessage());
        // Для donation flow возвращаем понятное сообщение
        String errorMessage = e.getMessage().contains("Active wallet not found") 
            ? "Автор не привязал кошелёк" 
            : e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", errorMessage));
    }

    /**
     * Обработка IllegalStateException (400)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        LOGGER.warn("⚠️ Некорректное состояние: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Обработка IllegalArgumentException (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        LOGGER.warn("⚠️ Некорректные данные: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("rootCause", getRootCauseMessage(ex));
        body.put("timestamp", java.time.OffsetDateTime.now());
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Обработка отсутствующих статических ресурсов (404)
     * Например, favicon.ico - браузеры автоматически запрашивают его
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        String resourcePath = ex.getResourcePath();
        
        // Для favicon.ico логируем на уровне DEBUG, так как это нормальный запрос браузера
        if (resourcePath != null && resourcePath.contains("favicon.ico")) {
            LOGGER.debug("🔍 Запрос favicon.ico (ресурс не найден, это нормально)");
            // Возвращаем пустой ответ с 404, браузер просто не покажет иконку
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // Для других отсутствующих ресурсов логируем на уровне WARN
        LOGGER.warn("⚠️ Статический ресурс не найден: {}", resourcePath);
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Not Found");
        body.put("message", "Ресурс не найден: " + resourcePath);
        body.put("timestamp", java.time.OffsetDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Обработка RuntimeException (500)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        LOGGER.error("❌ Внутренняя ошибка: {}", ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        body.put("rootCause", getRootCauseMessage(ex));
        body.put("timestamp", java.time.OffsetDateTime.now());
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Fallback для любых непойманных исключений (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex) {
        LOGGER.error("❌ Необработанная ошибка: {}", ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        body.put("rootCause", getRootCauseMessage(ex));
        body.put("timestamp", java.time.OffsetDateTime.now());
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause != null ? cause.getMessage() : null;
    }
}
