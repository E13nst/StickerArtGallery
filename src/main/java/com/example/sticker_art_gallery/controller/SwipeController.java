package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.SwipeStatsDto;
import com.example.sticker_art_gallery.model.swipe.SwipeConfigEntity;
import com.example.sticker_art_gallery.service.swipe.SwipeConfigService;
import com.example.sticker_art_gallery.service.swipe.SwipeTrackingService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления системой отслеживания свайпов
 */
@RestController
@RequestMapping("/api/swipes")
@Tag(name = "Swipes", description = "API для управления системой отслеживания свайпов")
@SecurityRequirement(name = "TelegramInitData")
public class SwipeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwipeController.class);

    private final SwipeTrackingService swipeTrackingService;
    private final SwipeConfigService swipeConfigService;

    public SwipeController(SwipeTrackingService swipeTrackingService,
                          SwipeConfigService swipeConfigService) {
        this.swipeTrackingService = swipeTrackingService;
        this.swipeConfigService = swipeConfigService;
    }

    /**
     * Получить статистику свайпов текущего пользователя
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Получить статистику свайпов",
        description = "Возвращает статистику свайпов текущего пользователя: количество свайпов за сегодня, " +
                     "дневной лимит, оставшиеся свайпы, информацию о подписке и прогресс до следующей награды."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статистика успешно получена",
            content = @Content(schema = @Schema(implementation = SwipeStatsDto.class),
                examples = @ExampleObject(value = """
                    {
                        "dailySwipes": 25,
                        "dailyLimit": 50,
                        "remainingSwipes": 25,
                        "hasSubscription": false,
                        "subscriptionExpiresAt": null,
                        "swipesPerReward": 50,
                        "swipesUntilReward": 25
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<SwipeStatsDto> getSwipeStats() {
        try {
            Long userId = getCurrentUserId();
            LOGGER.debug("📊 Получение статистики свайпов для пользователя {}", userId);

            SwipeTrackingService.SwipeStats stats = swipeTrackingService.getDailyStats(userId);
            SwipeStatsDto dto = new SwipeStatsDto(
                stats.getDailySwipes(),
                stats.getDailyLimit(),
                stats.getRemainingSwipes(),
                stats.isHasSubscription(),
                stats.getSubscriptionExpiresAt(),
                stats.getSwipesPerReward(),
                stats.getSwipesUntilReward()
            );

            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Пользователь не авторизован: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении статистики свайпов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить конфигурацию системы свайпов (только для админов)
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Получить конфигурацию системы свайпов",
        description = "Возвращает текущую конфигурацию системы отслеживания свайпов. Только для администраторов."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Конфигурация успешно получена",
            content = @Content(schema = @Schema(implementation = SwipeConfigEntity.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав доступа (только для администраторов)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<SwipeConfigEntity> getSwipeConfig() {
        try {
            LOGGER.debug("📋 Получение конфигурации системы свайпов");
            SwipeConfigEntity config = swipeConfigService.getActiveConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении конфигурации свайпов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Обновить конфигурацию системы свайпов (только для админов)
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Обновить конфигурацию системы свайпов",
        description = "Обновляет конфигурацию системы отслеживания свайпов. Только для администраторов."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Конфигурация успешно обновлена",
            content = @Content(schema = @Schema(implementation = SwipeConfigEntity.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав доступа (только для администраторов)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<SwipeConfigEntity> updateSwipeConfig(
            @Parameter(description = "Конфигурация для обновления", required = true)
            @RequestBody SwipeConfigEntity config) {
        try {
            LOGGER.info("📝 Обновление конфигурации системы свайпов");
            SwipeConfigEntity updated = swipeConfigService.updateConfig(config);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректные данные для обновления конфигурации: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении конфигурации свайпов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Извлечь ID текущего пользователя из SecurityContext
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        String nameStr = authentication.getName();
        Long telegramId = null;

        try {
            if (nameStr.matches("\\d+")) {
                telegramId = Long.parseLong(nameStr);
            } else {
                LOGGER.warn("⚠️ Получен неожиданный формат имени пользователя: {}", nameStr);
                if (nameStr.contains("id=")) {
                    String[] parts = nameStr.split("id=");
                    if (parts.length > 1) {
                        String idPart = parts[1].split(",")[0];
                        telegramId = Long.parseLong(idPart);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка парсинга telegramId: {}", nameStr, e);
            throw new IllegalStateException("Не удалось извлечь telegramId из аутентификации");
        }

        if (telegramId == null) {
            throw new IllegalStateException("Не удалось извлечь telegramId из аутентификации");
        }

        return telegramId;
    }
}
