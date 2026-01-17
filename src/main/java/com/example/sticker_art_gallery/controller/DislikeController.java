package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.DislikeDto;
import com.example.sticker_art_gallery.dto.DislikeResponseDto;
import com.example.sticker_art_gallery.dto.DislikeToggleResult;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.service.DislikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления дизлайками стикерсетов
 */
@RestController
@RequestMapping("/api/dislikes")
@Tag(name = "Dislikes", description = "API для управления дизлайками стикерсетов")
public class DislikeController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DislikeController.class);
    
    private final DislikeService dislikeService;
    
    public DislikeController(DislikeService dislikeService) {
        this.dislikeService = dislikeService;
    }
    
    /**
     * Поставить дизлайк стикерсету
     */
    @PostMapping("/stickersets/{stickerSetId}")
    @Operation(
        summary = "Поставить дизлайк стикерсету",
        description = "Добавляет дизлайк от текущего пользователя к указанному стикерсету. " +
                     "Если пользователь уже дизлайкнул этот стикерсет, возвращается ошибка. " +
                     "Если у пользователя есть лайк на этот стикерсет, он будет автоматически удален."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Дизлайк успешно поставлен",
            content = @Content(schema = @Schema(implementation = DislikeResponseDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "stickerSetId": 5,
                        "createdAt": "2025-01-15T10:30:00",
                        "disliked": true,
                        "totalDislikes": 1
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или стикерсет уже дизлайкнут"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<DislikeResponseDto> dislikeStickerSet(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("👎 Пользователь {} ставит дизлайк стикерсету {}", userId, stickerSetId);
            
            DislikeResponseDto result = dislikeService.dislikeStickerSet(userId, stickerSetId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Пользователь не авторизован: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при постановке дизлайка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при постановке дизлайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Убрать дизлайк со стикерсета
     */
    @DeleteMapping("/stickersets/{stickerSetId}")
    @Operation(
        summary = "Убрать дизлайк со стикерсета",
        description = "Удаляет дизлайк текущего пользователя с указанного стикерсета. " +
                     "Если дизлайк не найден, возвращается ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Дизлайк успешно убран",
            content = @Content(schema = @Schema(implementation = DislikeResponseDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "stickerSetId": 5,
                        "createdAt": "2025-01-15T10:30:00",
                        "disliked": false,
                        "totalDislikes": 0
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Дизлайк не найден"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<DislikeResponseDto> undislikeStickerSet(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("💔 Пользователь {} убирает дизлайк со стикерсета {}", userId, stickerSetId);
            
            DislikeResponseDto result = dislikeService.undislikeStickerSet(userId, stickerSetId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Пользователь не авторизован: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при удалении дизлайка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при удалении дизлайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Переключить дизлайк стикерсета
     */
    @PutMapping("/stickersets/{stickerSetId}/toggle")
    @Operation(
        summary = "Переключить дизлайк стикерсета",
        description = "Переключает состояние дизлайка: если дизлайк есть - убирает его, если нет - ставит. " +
                     "Возвращает текущее состояние дизлайка и общее количество дизлайков стикерсета."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Дизлайк успешно переключен",
            content = @Content(schema = @Schema(implementation = DislikeToggleResult.class),
                examples = @ExampleObject(value = """
                    {
                        "disliked": true,
                        "totalDislikes": 42
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<DislikeToggleResult> toggleDislike(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("🔄 Пользователь {} переключает дизлайк стикерсета {}", userId, stickerSetId);
            
            DislikeToggleResult result = dislikeService.toggleDislike(userId, stickerSetId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Пользователь не авторизован: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при переключении дизлайка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при переключении дизлайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить количество дизлайков для стикерсета по ID (без авторизации)
     */
    @GetMapping("/stickersets/{stickerSetId}")
    @Operation(
        summary = "Получить количество дизлайков для стикерсета",
        description = "Возвращает количество дизлайков для указанного стикерсета. " +
                     "Доступен без авторизации. Если пользователь не авторизован, возвращает 'disliked': false."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация о дизлайках успешно получена",
            content = @Content(schema = @Schema(implementation = DislikeToggleResult.class),
                examples = @ExampleObject(value = """
                    {
                        "disliked": false,
                        "totalDislikes": 5
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<DislikeToggleResult> getDislikesCount(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            LOGGER.debug("📊 Получение количества дизлайков для стикерсета {}", stickerSetId);
            
            // Проверяем существование стикерсета
            if (!dislikeService.stickerSetExists(stickerSetId)) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден", stickerSetId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            long totalDislikes = dislikeService.getDislikesCount(stickerSetId);
            
            // Проверяем, авторизован ли пользователь
            Long userId = getCurrentUserIdOrNull();
            boolean disliked = false;
            if (userId != null) {
                disliked = dislikeService.isDislikedByUser(userId, stickerSetId);
            }
            
            DislikeToggleResult result = new DislikeToggleResult(disliked, totalDislikes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении количества дизлайков: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить все дизлайки текущего пользователя
     */
    @GetMapping
    @Operation(
        summary = "Получить все дизлайки текущего пользователя",
        description = "Возвращает список всех дизлайков текущего пользователя с пагинацией."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список дизлайков успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "content": [
                            {
                                "id": 1,
                                "userId": 123456789,
                                "stickerSetId": 5,
                                "createdAt": "2025-01-15T10:30:00"
                            }
                        ],
                        "totalElements": 1,
                        "totalPages": 1,
                        "size": 20,
                        "number": 0,
                        "first": true,
                        "last": true,
                        "numberOfElements": 1
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<DislikeDto>> getUserDislikes(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.debug("📋 Получение дизлайков пользователя {}", userId);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("createdAt");
            pageRequest.setDirection("DESC");
            PageResponse<DislikeDto> result = dislikeService.getUserDislikes(userId, pageRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении дизлайков пользователя: {}", e.getMessage(), e);
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
        
        // Извлекаем telegramId из имени пользователя
        String nameStr = authentication.getName();
        Long telegramId = null;
        
        try {
            // Если имя - это просто число (telegramId)
            if (nameStr.matches("\\d+")) {
                telegramId = Long.parseLong(nameStr);
            } else {
                // Если имя содержит UserEntity объект, парсим его
                LOGGER.warn("⚠️ Получен неожиданный формат имени пользователя: {}", nameStr);
                // Попробуем извлечь telegramId из строки
                if (nameStr.contains("id=")) {
                    String[] parts = nameStr.split("id=");
                    if (parts.length > 1) {
                        String idPart = parts[1].split(",")[0];
                        telegramId = Long.parseLong(idPart);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка парсинга telegramId из authentication.getName(): {}", nameStr, e);
            throw new IllegalStateException("Не удалось извлечь telegramId из аутентификации");
        }
        
        if (telegramId == null) {
            throw new IllegalStateException("Не удалось извлечь telegramId из аутентификации");
        }
        
        return telegramId;
    }
    
    /**
     * Извлечь ID текущего пользователя из SecurityContext (может вернуть null)
     */
    private Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
