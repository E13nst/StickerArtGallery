package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.UserDto;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер для работы с пользователями
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Пользователи", description = "Управление пользователями системы")
@SecurityRequirement(name = "TelegramInitData")
public class UserController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Получить всех пользователей
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Получить всех пользователей",
        description = "Возвращает список всех пользователей системы (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список пользователей получен",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    [
                        {
                            "id": 123456789,
                            "username": "testuser",
                            "firstName": "Test",
                            "lastName": "User",
                            "role": "USER",
                            "artBalance": 0
                        }
                    ]
                    """))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        try {
            LOGGER.info("📋 Получение всех пользователей");
            List<UserEntity> userEntities = userService.findAll();
            List<UserDto> users = userService.enrichUsersSafely(userEntities);
            LOGGER.info("✅ Найдено {} пользователей", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении пользователей: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить пользователя по ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Получить пользователя по ID",
        description = "Возвращает информацию о пользователе по его ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 123456789,
                        "username": "testuser",
                        "firstName": "Test",
                        "lastName": "User",
                        "role": "USER",
                        "artBalance": 0,
                        "telegramUserInfo": {...},
                        "profilePhotos": {...},
                        "profilePhotoFileId": "AgACAgIAAxkBAAIBY2..."
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "ID пользователя (Telegram ID)", required = true, example = "123456789")
            @PathVariable Long id) {
        try {
            LOGGER.info("🔍 Поиск пользователя по ID: {}", id);
            Optional<UserEntity> userOpt = userService.findById(id);
            
            if (userOpt.isPresent()) {
                UserDto userDto = userService.enrichSingleUserSafely(userOpt.get());
                LOGGER.info("✅ Пользователь найден: {}", userDto.getUsername());
                return ResponseEntity.ok(userDto);
            } else {
                LOGGER.warn("⚠️ Пользователь с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске пользователя с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить пользователя по username
     */
    @GetMapping("/username/{username}")
    @Operation(
        summary = "Получить пользователя по username",
        description = "Возвращает информацию о пользователе по его username"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> getUserByUsername(
            @Parameter(description = "Username пользователя", required = true, example = "testuser")
            @PathVariable String username) {
        try {
            LOGGER.info("🔍 Поиск пользователя по username: {}", username);
            Optional<UserEntity> userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                UserDto userDto = userService.enrichSingleUserSafely(userOpt.get());
                LOGGER.info("✅ Пользователь найден: {}", userDto.getUsername());
                return ResponseEntity.ok(userDto);
            } else {
                LOGGER.warn("⚠️ Пользователь с username {} не найден", username);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске пользователя с username {}: {}", username, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Создать нового пользователя
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Создать нового пользователя",
        description = "Создает нового пользователя в системе. ID пользователя должен быть Telegram ID. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 999999999,
                        "username": "newuser123",
                        "firstName": "New",
                        "lastName": "User",
                        "avatarUrl": "https://example.com/avatar.jpg",
                        "artBalance": 100,
                        "role": "USER",
                        "createdAt": "2025-09-15T14:30:00",
                        "updatedAt": "2025-09-15T14:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные - ошибки валидации",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "validationErrors": {
                        "id": "ID (Telegram ID) должен быть положительным числом",
                        "username": "Username может содержать только буквы, цифры и подчеркивания",
                        "avatarUrl": "URL аватара должен начинаться с http:// или https://",
                        "artBalance": "Баланс арт-кредитов не может быть отрицательным",
                        "role": "Роль должна быть USER или ADMIN"
                    },
                    "error": "Ошибка валидации",
                    "message": "Некорректные данные в запросе"
                }
                """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true)
            @Valid @RequestBody UserDto userDto) {
        try {
            LOGGER.info("🆕 Создание нового пользователя: {} (ID: {})", userDto.getUsername(), userDto.getId());
            
            // Проверяем, что пользователь с таким ID не существует
            if (userService.existsByTelegramId(userDto.getId())) {
                LOGGER.warn("⚠️ Пользователь с ID {} уже существует", userDto.getId());
                return ResponseEntity.badRequest().build();
            }
            
            UserEntity userEntity = userDto.toEntity();
            UserEntity savedUser = userService.save(userEntity);
            UserDto savedUserDto = userService.enrichSingleUserSafely(savedUser);
            
            LOGGER.info("✅ Пользователь создан: {} (ID: {})", savedUserDto.getUsername(), savedUserDto.getId());
            return ResponseEntity.status(201).body(savedUserDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании пользователя: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Обновить баланс пользователя
     */
    @PatchMapping("/{id}/balance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Обновить баланс пользователя",
        description = "Обновляет баланс арт-кредитов пользователя (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Баланс обновлен"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> updateUserBalance(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Новый баланс", required = true, example = "100")
            @Valid @RequestBody @Min(value = 0, message = "Баланс не может быть отрицательным") Long newBalance) {
        try {
            LOGGER.info("💰 Обновление баланса пользователя {}: {}", id, newBalance);
            UserEntity updatedUser = userService.updateArtBalance(id, newBalance);
            UserDto userDto = userService.enrichSingleUserSafely(updatedUser);
            LOGGER.info("✅ Баланс обновлен для пользователя: {}", userDto.getUsername());
            return ResponseEntity.ok(userDto);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Пользователь с ID {} не найден", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении баланса пользователя {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Добавить к балансу пользователя
     */
    @PostMapping("/{id}/balance/add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Добавить к балансу пользователя",
        description = "Добавляет указанное количество арт-кредитов к балансу пользователя (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Баланс обновлен"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> addToUserBalance(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Количество для добавления", required = true, example = "50")
            @RequestBody Long amount) {
        try {
            LOGGER.info("💰 Добавление к балансу пользователя {}: {}", id, amount);
            UserEntity updatedUser = userService.addToArtBalance(id, amount);
            UserDto userDto = userService.enrichSingleUserSafely(updatedUser);
            LOGGER.info("✅ Баланс обновлен для пользователя: {}", userDto.getUsername());
            return ResponseEntity.ok(userDto);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Пользователь с ID {} не найден", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при добавлении к балансу пользователя {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Удалить пользователя
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить пользователя",
        description = "Удаляет пользователя из системы. Администратор может удалять любых пользователей, обычный пользователь - только свой аккаунт."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Пользователь удален"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно удалять только свой аккаунт"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", required = true, example = "123456789")
            @PathVariable Long id) {
        try {
            LOGGER.info("🗑️ Удаление пользователя с ID: {}", id);
            
            // Проверяем, существует ли пользователь
            if (!userService.findById(id).isPresent()) {
                LOGGER.warn("⚠️ Пользователь с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            // Проверяем права доступа
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof com.example.sticker_art_gallery.model.user.UserEntity) {
                com.example.sticker_art_gallery.model.user.UserEntity currentUser = 
                    (com.example.sticker_art_gallery.model.user.UserEntity) authentication.getPrincipal();
                
                // Проверяем: админ или удаляет свой аккаунт
                boolean isAdmin = currentUser.getRole() == com.example.sticker_art_gallery.model.user.UserEntity.UserRole.ADMIN;
                boolean isSelf = id.equals(currentUser.getId());
                
                if (!isAdmin && !isSelf) {
                    LOGGER.warn("⚠️ Пользователь {} попытался удалить чужой аккаунт {}", currentUser.getId(), id);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
                
                LOGGER.debug("✅ Проверка прав пройдена: isAdmin={}, isSelf={}", isAdmin, isSelf);
            }
            
            userService.deleteById(id);
            LOGGER.info("✅ Пользователь с ID {} удален", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении пользователя с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
