package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.util.TelegramInitDataValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для аутентификации
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "Эндпоинты для аутентификации через Telegram Web App")
public class AuthController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
    private static final String DEFAULT_BOT_NAME = "StickerGallery";
    
    private final TelegramInitDataValidator validator;
    
    @Autowired
    public AuthController(TelegramInitDataValidator validator) {
        this.validator = validator;
    }
    
    /**
     * Проверка статуса аутентификации
     */
    @GetMapping("/status")
    @Operation(
        summary = "Проверка статуса аутентификации",
        description = "Возвращает текущий статус аутентификации пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статус получен успешно",
            content = @Content(schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "authenticated": true,
                        "telegramId": 123456789,
                        "username": "testuser",
                        "role": "USER"
                    }
                    """)))
    })
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> response = new HashMap<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getName())) {
            
            // Извлекаем telegramId из имени пользователя (которое содержит telegramId)
            String nameStr = authentication.getName();
            Long telegramId = null;
            String username = null;
            
            try {
                // Имя теперь всегда содержит telegramId
                telegramId = Long.parseLong(nameStr);
                username = "user_" + telegramId;
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка парсинга telegramId из authentication.getName(): {}", nameStr, e);
            }
            
            response.put("authenticated", true);
            response.put("telegramId", telegramId);
            response.put("username", username);
            response.put("role", authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElse("ROLE_USER"));
            
            LOGGER.info("✅ Статус аутентификации: пользователь {} (telegramId: {}) аутентифицирован", username, telegramId);
        } else {
            response.put("authenticated", false);
            response.put("message", "No authentication data provided");
            LOGGER.debug("❌ Статус аутентификации: пользователь не аутентифицирован");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Валидация initData для конкретного бота
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Валидация Telegram initData",
        description = "Проверяет валидность Telegram Web App initData для конкретного бота"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Валидация выполнена успешно",
            content = @Content(schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "valid": true,
                        "botName": "StickerGallery",
                        "telegramId": 123456789,
                        "message": "InitData is valid for bot: StickerGallery"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "valid": false,
                        "error": "Invalid initData for bot: StickerGallery"
                    }
                    """)))
    })
    public ResponseEntity<Map<String, Object>> validateInitData(
            @Parameter(description = "Данные для валидации", required = true,
                content = @Content(examples = @ExampleObject(value = """
                    {
                        "initData": "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=abc123...",
                        "botName": "StickerGallery"
                    }
                    """)))
            @RequestBody Map<String, String> request) {
        String initData = request.get("initData");
        String botNameRaw = request.get("botName");
        String botName = resolveBotName(botNameRaw);
        boolean usingDefaultBot = botNameRaw == null || botNameRaw.trim().isEmpty();
        
        Map<String, Object> response = new HashMap<>();
        
        if (initData == null || initData.trim().isEmpty()) {
            response.put("valid", false);
            response.put("error", "InitData is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (usingDefaultBot) {
            LOGGER.debug("Используется имя бота по умолчанию '{}' для проверки initData", DEFAULT_BOT_NAME);
        }

        boolean isValid = validator.validateInitData(initData, botName);
        response.put("valid", isValid);
        response.put("botName", botName);
        
        if (isValid) {
            Long telegramId = validator.extractTelegramId(initData);
            response.put("telegramId", telegramId);
            response.put("message", "InitData is valid for bot: " + botName);
        } else {
            response.put("error", "Invalid initData for bot: " + botName);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получение информации о пользователе по initData и имени бота
     */
    @PostMapping("/user")
    @Operation(
        summary = "Получение информации о пользователе",
        description = "Находит пользователя по Telegram initData и возвращает его информацию"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден",
            content = @Content(schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "user": {
                            "id": 1,
                            "telegramId": 123456789,
                            "username": "testuser",
                            "firstName": "Test",
                            "lastName": "User",
                            "role": "USER",
                            "artBalance": 0
                        },
                        "botName": "StickerGallery",
                        "message": "User found for bot: StickerGallery"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @Parameter(description = "Данные для поиска пользователя", required = true)
            @RequestBody Map<String, String> request) {
        String initData = request.get("initData");
        String botNameRaw = request.get("botName");
        String botName = resolveBotName(botNameRaw);
        boolean usingDefaultBot = botNameRaw == null || botNameRaw.trim().isEmpty();
        
        Map<String, Object> response = new HashMap<>();
        
        if (initData == null || initData.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "InitData is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            if (usingDefaultBot) {
                LOGGER.debug("Используется имя бота по умолчанию '{}' для получения информации о пользователе", DEFAULT_BOT_NAME);
            }
            // Валидируем initData для конкретного бота
            if (!validator.validateInitData(initData, botName)) {
                response.put("success", false);
                response.put("error", "Invalid initData for bot: " + botName);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Извлекаем telegram_id
            Long telegramId = validator.extractTelegramId(initData);
            if (telegramId == null) {
                response.put("success", false);
                response.put("error", "Could not extract telegram_id from initData");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Возвращаем базовую информацию
            response.put("success", true);
            response.put("telegramId", telegramId);
            response.put("botName", botName);
            response.put("message", "User validated for bot: " + botName);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка получения информации о пользователе для бота {}: {}", botName, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Internal server error");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Создание пользователя по initData и имени бота
     */
    @PostMapping("/register")
    @Operation(
        summary = "Регистрация пользователя",
        description = "Создает нового пользователя или находит существующего по Telegram initData"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь зарегистрирован",
            content = @Content(schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "user": {
                            "id": 1,
                            "telegramId": 123456789,
                            "username": "testuser",
                            "firstName": "Test",
                            "lastName": "User",
                            "role": "USER",
                            "artBalance": 0
                        },
                        "botName": "StickerGallery",
                        "message": "User registered successfully for bot: StickerGallery"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Map<String, Object>> registerUser(
            @Parameter(description = "Данные для регистрации пользователя", required = true)
            @RequestBody Map<String, String> request) {
        String initData = request.get("initData");
        String botNameRaw = request.get("botName");
        String botName = resolveBotName(botNameRaw);
        boolean usingDefaultBot = botNameRaw == null || botNameRaw.trim().isEmpty();
        
        Map<String, Object> response = new HashMap<>();
        
        if (initData == null || initData.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "InitData is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            if (usingDefaultBot) {
                LOGGER.debug("Используется имя бота по умолчанию '{}' для регистрации пользователя", DEFAULT_BOT_NAME);
            }
            // Валидируем initData для конкретного бота
            if (!validator.validateInitData(initData, botName)) {
                response.put("success", false);
                response.put("error", "Invalid initData for bot: " + botName);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Извлекаем telegram_id
            Long telegramId = validator.extractTelegramId(initData);
            if (telegramId == null) {
                response.put("success", false);
                response.put("error", "Could not extract telegram_id from initData");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Пользователь будет создан автоматически при аутентификации
            response.put("success", true);
            response.put("telegramId", telegramId);
            response.put("botName", botName);
            response.put("message", "User registered automatically on first authentication for bot: " + botName);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка регистрации пользователя для бота {}: {}", botName, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Internal server error");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    private String resolveBotName(String botName) {
        if (botName == null || botName.trim().isEmpty()) {
            return DEFAULT_BOT_NAME;
        }
        return botName.trim();
    }
    
}
