package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.transaction.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * Контроллер для управления видимостью стикерсетов
 */
@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*")
@Tag(name = "Видимость стикерсетов", description = "Управление видимостью стикерсетов (публикация, скрытие, блокировка)")
@SecurityRequirement(name = "TelegramInitData")
public class StickerSetVisibilityController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetVisibilityController.class);
    private final StickerSetService stickerSetService;
    private final WalletService walletService;
    private final StickerSetControllerHelper helper;
    
    @Autowired
    public StickerSetVisibilityController(StickerSetService stickerSetService,
                                         WalletService walletService,
                                         StickerSetControllerHelper helper) {
        this.stickerSetService = stickerSetService;
        this.walletService = walletService;
        this.helper = helper;
    }
    
    /**
     * Опубликовать стикерсет в галерее (сделать публичным)
     */
    @PostMapping("/{id}/publish")
    @Operation(
        summary = "Опубликовать стикерсет в галерее",
        description = "Делает стикерсет публичным - видимым для всех пользователей в галерее. " +
                     "Администратор может публиковать любые стикерсеты, обычный пользователь - только свои."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет успешно опубликован",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "title": "Мои стикеры",
                        "name": "my_stickers_by_StickerGalleryBot",
                        "isPublic": true,
                        "createdAt": "2025-09-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно публиковать только свои стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> publishStickerSet(
            @Parameter(description = "ID стикерсета для публикации", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        return updateStickerSetVisibilityInternal(id, true, "опубликован");
    }
    
    /**
     * Скрыть стикерсет из галереи (сделать приватным)
     */
    @PostMapping("/{id}/unpublish")
    @Operation(
        summary = "Скрыть стикерсет из галереи",
        description = "Делает стикерсет приватным - видимым только владельцу в его профиле. " +
                     "Администратор может скрывать любые стикерсеты, обычный пользователь - только свои."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет успешно скрыт",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "title": "Мои стикеры",
                        "name": "my_stickers_by_StickerGalleryBot",
                        "isPublic": false,
                        "createdAt": "2025-09-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно скрывать только свои стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> unpublishStickerSet(
            @Parameter(description = "ID стикерсета для скрытия", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        return updateStickerSetVisibilityInternal(id, false, "скрыт");
    }
    
    /**
     * Заблокировать стикерсет (только для админа)
     */
    @PutMapping("/{id}/block")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Заблокировать стикерсет",
        description = "Блокирует стикерсет (доступно только админу). " +
                     "Заблокированные стикерсеты не отображаются в галерее и в профилях пользователей. " +
                     "Параметр reason (причина блокировки) опционален и по умолчанию пустой."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет успешно заблокирован",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "title": "Мои стикеры",
                        "name": "my_stickers_by_StickerGalleryBot",
                        "isPublic": true,
                        "isBlocked": true,
                        "blockReason": null,
                        "createdAt": "2025-09-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может блокировать стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> blockStickerSet(
            @Parameter(description = "ID стикерсета для блокировки", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Причина блокировки (опционально, по умолчанию пустая)", required = false)
            @RequestBody(required = false) Map<String, String> request) {
        try {
            LOGGER.info("🚫 Блокировка стикерсета с ID: {}", id);
            
            String reason = request != null ? request.get("reason") : null;
            if (reason != null && reason.trim().isEmpty()) {
                reason = null;
            }
            
            // Получаем currentUserId и isAdmin (эндпоинт доступен только админам)
            Long currentUserId = helper.getCurrentUserIdOrNull();
            boolean isAdmin = true; // Эндпоинт доступен только админам через @PreAuthorize
            
            StickerSet blockedStickerSet = stickerSetService.blockStickerSet(id, reason);
            StickerSetDto blockedDto = StickerSetDto.fromEntity(blockedStickerSet);
            
            // Проверяем наличие TON кошелька
            boolean hasTonWallet = false;
            if (currentUserId != null) {
                try {
                    hasTonWallet = walletService.hasActiveWallet(currentUserId);
                } catch (Exception e) {
                    LOGGER.debug("⚠️ Ошибка при проверке наличия кошелька для пользователя {}: {}", currentUserId, e.getMessage());
                    hasTonWallet = false;
                }
            }
            
            // Устанавливаем availableActions
            blockedDto.setAvailableActions(StickerSetDto.calculateAvailableActions(
                currentUserId,
                isAdmin,
                blockedStickerSet.getUserId(),
                blockedStickerSet.getIsVerified(),
                blockedStickerSet.getState(),
                blockedStickerSet.getVisibility(),
                hasTonWallet
            ));
            
            LOGGER.info("✅ Стикерсет {} заблокирован по причине: {}", id, reason);
            return ResponseEntity.ok(blockedDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при блокировке стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Ошибка валидации",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при блокировке стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Внутренняя ошибка сервера",
                    "message", "Произошла непредвиденная ошибка при блокировке стикерсета"
                ));
        }
    }
    
    /**
     * Разблокировать стикерсет (только для админа)
     */
    @PutMapping("/{id}/unblock")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Разблокировать стикерсет",
        description = "Разблокирует стикерсет (доступно только админу). " +
                     "Стикерсет снова становится доступным в галерее (если он публичный)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет успешно разблокирован",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "title": "Мои стикеры",
                        "name": "my_stickers_by_StickerGalleryBot",
                        "isPublic": true,
                        "isBlocked": false,
                        "blockReason": null,
                        "createdAt": "2025-09-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может разблокировать стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> unblockStickerSet(
            @Parameter(description = "ID стикерсета для разблокировки", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("✅ Разблокировка стикерсета с ID: {}", id);
            
            // Получаем currentUserId и isAdmin (эндпоинт доступен только админам)
            Long currentUserId = helper.getCurrentUserIdOrNull();
            boolean isAdmin = true; // Эндпоинт доступен только админам через @PreAuthorize
            
            StickerSet unblockedStickerSet = stickerSetService.unblockStickerSet(id);
            StickerSetDto unblockedDto = StickerSetDto.fromEntity(unblockedStickerSet);
            
            // Проверяем наличие TON кошелька
            boolean hasTonWallet = false;
            if (currentUserId != null) {
                try {
                    hasTonWallet = walletService.hasActiveWallet(currentUserId);
                } catch (Exception e) {
                    LOGGER.debug("⚠️ Ошибка при проверке наличия кошелька для пользователя {}: {}", currentUserId, e.getMessage());
                    hasTonWallet = false;
                }
            }
            
            // Устанавливаем availableActions
            unblockedDto.setAvailableActions(StickerSetDto.calculateAvailableActions(
                currentUserId,
                isAdmin,
                unblockedStickerSet.getUserId(),
                unblockedStickerSet.getIsVerified(),
                unblockedStickerSet.getState(),
                unblockedStickerSet.getVisibility(),
                hasTonWallet
            ));
            
            LOGGER.info("✅ Стикерсет {} разблокирован", id);
            return ResponseEntity.ok(unblockedDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при разблокировке стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Ошибка валидации",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при разблокировке стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Внутренняя ошибка сервера",
                    "message", "Произошла непредвиденная ошибка при разблокировке стикерсета"
                ));
        }
    }
    
    /**
     * Внутренний метод для изменения видимости стикерсета
     */
    private ResponseEntity<?> updateStickerSetVisibilityInternal(Long id, Boolean isPublic, String action) {
        try {
            LOGGER.info("👁️ Изменение видимости стикерсета с ID: {} на {}", id, isPublic ? "публичный" : "приватный");
            
            StickerSet existingStickerSet = stickerSetService.findById(id);
            if (existingStickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден для изменения видимости", id);
                return ResponseEntity.notFound().build();
            }
            
            // Проверяем права доступа
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long currentUserId = null;
            boolean isAdmin = false;
            
            if (authentication != null && authentication.isAuthenticated()) {
                currentUserId = Long.valueOf(authentication.getName());
                
                // Проверяем: админ или владелец стикерсета
                isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                boolean isOwner = existingStickerSet.getUserId() != null && existingStickerSet.getUserId().equals(currentUserId);
                
                if (!isAdmin && !isOwner) {
                    LOGGER.warn("⚠️ Пользователь {} попытался изменить видимость чужого стикерсета {}", currentUserId, id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "error", "Доступ запрещен",
                            "message", "Вы можете изменять видимость только своих стикерсетов"
                        ));
                }
                
                LOGGER.debug("✅ Проверка прав на изменение видимости пройдена: isAdmin={}, isOwner={}", isAdmin, isOwner);
            }
            
            StickerSet updatedStickerSet;
            if (isPublic) {
                updatedStickerSet = stickerSetService.publishStickerSet(id);
            } else {
                updatedStickerSet = stickerSetService.unpublishStickerSet(id);
            }
            StickerSetDto updatedDto = StickerSetDto.fromEntity(updatedStickerSet);
            
            // Проверяем наличие TON кошелька
            boolean hasTonWallet = false;
            if (currentUserId != null) {
                try {
                    hasTonWallet = walletService.hasActiveWallet(currentUserId);
                } catch (Exception e) {
                    LOGGER.debug("⚠️ Ошибка при проверке наличия кошелька для пользователя {}: {}", currentUserId, e.getMessage());
                    hasTonWallet = false;
                }
            }
            
            // Устанавливаем availableActions
            updatedDto.setAvailableActions(StickerSetDto.calculateAvailableActions(
                currentUserId,
                isAdmin,
                updatedStickerSet.getUserId(),
                updatedStickerSet.getIsVerified(),
                updatedStickerSet.getState(),
                updatedStickerSet.getVisibility(),
                hasTonWallet
            ));
            
            LOGGER.info("✅ Стикерсет {} {}", id, action);
            return ResponseEntity.ok(updatedDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при изменении видимости стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "Ошибка валидации",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при изменении видимости стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Внутренняя ошибка сервера",
                    "message", "Произошла непредвиденная ошибка при изменении видимости стикерсета"
                ));
        }
    }
}
