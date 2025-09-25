package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.UserDto;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.service.user.UserService;
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
@RequestMapping("/auth")
@Tag(name = "Аутентификация", description = "Эндпоинты для аутентификации через Telegram Web App")
public class AuthController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
    
    private final TelegramInitDataValidator validator;
    private final UserService userService;
    
    @Autowired
    public AuthController(TelegramInitDataValidator validator, UserService userService) {
        this.validator = validator;
        this.userService = userService;
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
            
            response.put("authenticated", true);
            response.put("telegramId", authentication.getName());
            response.put("username", authentication.getName());
            response.put("role", authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElse("USER"));
            
            LOGGER.info("✅ Статус аутентификации: пользователь {} аутентифицирован", authentication.getName());
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
        String botName = request.get("botName");
        
        Map<String, Object> response = new HashMap<>();
        
        if (initData == null || initData.trim().isEmpty()) {
            response.put("valid", false);
            response.put("error", "InitData is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (botName == null || botName.trim().isEmpty()) {
            response.put("valid", false);
            response.put("error", "Bot name is required");
            return ResponseEntity.badRequest().body(response);
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
        String botName = request.get("botName");
        
        Map<String, Object> response = new HashMap<>();
        
        if (initData == null || initData.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "InitData is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (botName == null || botName.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Bot name is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
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
            
            // Находим пользователя
            var userOpt = userService.findByTelegramId(telegramId);
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                UserDto userDto = UserDto.fromEntity(user);
                
                response.put("success", true);
                response.put("user", userDto);
                response.put("botName", botName);
                response.put("message", "User found for bot: " + botName);
            } else {
                response.put("success", false);
                response.put("error", "User not found");
                response.put("telegramId", telegramId);
                response.put("botName", botName);
            }
            
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
        String botName = request.get("botName");
        
        Map<String, Object> response = new HashMap<>();
        
        if (initData == null || initData.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "InitData is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (botName == null || botName.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Bot name is required");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
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
            
            // Простой парсинг данных пользователя (в реальном проекте используйте JSON парсер)
            String username = extractValue(initData, "username");
            String firstName = extractValue(initData, "first_name");
            String lastName = extractValue(initData, "last_name");
            String photoUrl = extractValue(initData, "photo_url");
            
            // Создаем или находим пользователя
            UserEntity user = userService.findOrCreateByTelegramId(telegramId, username, firstName, lastName, photoUrl);
            UserDto userDto = UserDto.fromEntity(user);
            
            response.put("success", true);
            response.put("user", userDto);
            response.put("botName", botName);
            response.put("message", "User registered successfully for bot: " + botName);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка регистрации пользователя для бота {}: {}", botName, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Internal server error");
            return ResponseEntity.internalServerError().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Простой метод извлечения значения из строки
     */
    private String extractValue(String initData, String fieldName) {
        try {
            int startIndex = initData.indexOf("\"" + fieldName + "\":\"");
            if (startIndex == -1) {
                return null;
            }
            
            startIndex = initData.indexOf("\"", startIndex + fieldName.length() + 3) + 1;
            int endIndex = initData.indexOf("\"", startIndex);
            
            if (endIndex == -1) {
                return null;
            }
            
            return initData.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
}
