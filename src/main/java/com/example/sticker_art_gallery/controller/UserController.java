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

import java.util.Optional;

/**
 * Контроллер для работы с данными пользователей из Telegram
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Пользователи", description = "Данные пользователей из Telegram")
@SecurityRequirement(name = "TelegramInitData")
public class UserController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Получить данные пользователя из Telegram по ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Получить данные пользователя из Telegram",
        description = "Возвращает данные пользователя из Telegram Bot API по его ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Данные пользователя получены",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 123456789,
                        "username": "testuser",
                        "firstName": "Test",
                        "lastName": "User",
                        "languageCode": "ru",
                        "isPremium": true,
                        "createdAt": "2025-10-20T10:00:00Z",
                        "updatedAt": "2025-10-20T10:00:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long id) {
        try {
            LOGGER.info("🔍 Получение данных пользователя по ID: {}", id);
            
            Optional<UserEntity> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                LOGGER.warn("⚠️ Пользователь с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            UserDto userDto = UserDto.fromEntity(userOpt.get());
            
            LOGGER.info("✅ Данные пользователя получены: {}", userDto.getUsername());
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении данных пользователя с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить фото профиля пользователя
     */
    @GetMapping("/{id}/photo")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Получить фото профиля пользователя",
        description = "Возвращает информацию о фото профиля пользователя из Telegram Bot API с file_id для скачивания"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Фото профиля получено",
            content = @Content(schema = @Schema(implementation = java.util.Map.class),
                examples = @ExampleObject(value = """
                    {
                        "profilePhotos": {
                            "total_count": 4,
                            "photos": [[{...}]]
                        },
                        "profilePhotoFileId": "AgACAgIAAxkBAAIBY2..."
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден или нет фото"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<java.util.Map<String, Object>> getUserPhoto(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long id) {
        try {
            LOGGER.info("📷 Получение фото профиля пользователя: {}", id);
            
            java.util.Map<String, Object> photoData = userService.getUserProfilePhoto(id);
            if (photoData == null) {
                LOGGER.warn("⚠️ Фото профиля для пользователя {} не найдено", id);
                return ResponseEntity.notFound().build();
            }
            
            LOGGER.info("✅ Фото профиля получено для пользователя: {}", id);
            return ResponseEntity.ok(photoData);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении фото профиля пользователя {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
