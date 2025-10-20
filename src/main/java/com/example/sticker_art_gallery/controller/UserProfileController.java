package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.UserProfileDto;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.Optional;

/**
 * Контроллер для работы с профилями пользователей
 */
@RestController
@RequestMapping("/api/profiles")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Профили пользователей", description = "Управление профилями пользователей")
@SecurityRequirement(name = "TelegramInitData")
public class UserProfileController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileController.class);
    
    private final UserProfileService userProfileService;
    
    @Autowired
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }
    
    /**
     * Получить профиль пользователя по ID
     */
    @GetMapping("/{userId}")
    @Operation(
        summary = "Получить профиль пользователя по ID",
        description = "Возвращает профиль пользователя по его Telegram ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Профиль найден",
            content = @Content(schema = @Schema(implementation = UserProfileDto.class),
                examples = @ExampleObject(value = """
                    {
                        "userId": 123456789,
                        "role": "USER",
                        "artBalance": 100,
                        "createdAt": "2025-01-15T10:30:00Z",
                        "updatedAt": "2025-01-15T14:30:00Z"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserProfileDto> getProfileById(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long userId) {
        try {
            LOGGER.info("🔍 Поиск профиля пользователя по ID: {}", userId);
            Optional<UserProfileEntity> profileOpt = userProfileService.findByTelegramId(userId);
            
            if (profileOpt.isPresent()) {
                UserProfileDto profileDto = UserProfileDto.fromEntity(profileOpt.get());
                LOGGER.info("✅ Профиль найден: userId={}, role={}, balance={}", 
                           profileDto.getUserId(), profileDto.getRole(), profileDto.getArtBalance());
                return ResponseEntity.ok(profileDto);
            } else {
                LOGGER.warn("⚠️ Профиль пользователя с ID {} не найден", userId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске профиля пользователя с ID {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить мой профиль
     */
    @GetMapping("/me")
    @Operation(
        summary = "Получить мой профиль",
        description = "Возвращает профиль текущего авторизованного пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Профиль найден"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserProfileDto> getMyProfile() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Не удалось определить ID текущего пользователя");
                return ResponseEntity.badRequest().build();
            }
            
            LOGGER.info("🔍 Получение профиля текущего пользователя: {}", currentUserId);
            Optional<UserProfileEntity> profileOpt = userProfileService.findByTelegramId(currentUserId);
            
            if (profileOpt.isPresent()) {
                UserProfileDto profileDto = UserProfileDto.fromEntity(profileOpt.get());
                LOGGER.info("✅ Профиль найден: userId={}, role={}, balance={}", 
                           profileDto.getUserId(), profileDto.getRole(), profileDto.getArtBalance());
                return ResponseEntity.ok(profileDto);
            } else {
                LOGGER.warn("⚠️ Профиль текущего пользователя {} не найден", currentUserId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении профиля текущего пользователя: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Обновить баланс пользователя
     */
    @PutMapping("/{userId}/balance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Обновить баланс пользователя",
        description = "Обновляет баланс арт-кредитов пользователя (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Баланс обновлен"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserProfileDto> updateUserBalance(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long userId,
            @Parameter(description = "Новый баланс", required = true, example = "100")
            @Valid @RequestBody @Min(value = 0, message = "Баланс не может быть отрицательным") Long newBalance) {
        try {
            LOGGER.info("💰 Обновление баланса пользователя {}: {}", userId, newBalance);
            UserProfileEntity updatedProfile = userProfileService.updateArtBalance(userId, newBalance);
            UserProfileDto profileDto = UserProfileDto.fromEntity(updatedProfile);
            LOGGER.info("✅ Баланс обновлен для пользователя: userId={}, newBalance={}", 
                       profileDto.getUserId(), profileDto.getArtBalance());
            return ResponseEntity.ok(profileDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении баланса пользователя {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Добавить к балансу пользователя
     */
    @PostMapping("/{userId}/balance/add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Добавить к балансу пользователя",
        description = "Добавляет указанное количество арт-кредитов к балансу пользователя (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Баланс обновлен"),
        @ApiResponse(responseCode = "404", description = "Профиль не найден"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserProfileDto> addToUserBalance(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long userId,
            @Parameter(description = "Количество для добавления", required = true, example = "50")
            @RequestBody Long amount) {
        try {
            LOGGER.info("💰 Добавление к балансу пользователя {}: {}", userId, amount);
            UserProfileEntity updatedProfile = userProfileService.addToArtBalance(userId, amount);
            UserProfileDto profileDto = UserProfileDto.fromEntity(updatedProfile);
            LOGGER.info("✅ Баланс обновлен для пользователя: userId={}, newBalance={}", 
                       profileDto.getUserId(), profileDto.getArtBalance());
            return ResponseEntity.ok(profileDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при добавлении к балансу пользователя {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Извлечь ID текущего пользователя из SecurityContext
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            return Long.valueOf(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
