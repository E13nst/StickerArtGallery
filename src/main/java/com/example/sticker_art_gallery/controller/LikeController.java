package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.LikeDto;
import com.example.sticker_art_gallery.dto.LikeToggleResult;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.StickerSetWithLikesDto;
import com.example.sticker_art_gallery.service.LikeService;
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
 * Контроллер для управления лайками стикерсетов
 */
@RestController
@RequestMapping("/api/likes")
@Tag(name = "Likes", description = "API для управления лайками стикерсетов")
public class LikeController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LikeController.class);
    
    private final LikeService likeService;
    
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }
    
    /**
     * Поставить лайк стикерсету
     */
    @PostMapping("/sticker-sets/{stickerSetId}")
    @Operation(
        summary = "Поставить лайк стикерсету",
        description = "Добавляет лайк от текущего пользователя к указанному стикерсету. " +
                     "Если пользователь уже лайкнул этот стикерсет, возвращается ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Лайк успешно поставлен",
            content = @Content(schema = @Schema(implementation = LikeDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "stickerSetId": 5,
                        "createdAt": "2025-01-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или стикерсет уже лайкнут"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<LikeDto> likeStickerSet(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("❤️ Пользователь {} ставит лайк стикерсету {}", userId, stickerSetId);
            
            LikeDto like = likeService.likeStickerSet(userId, stickerSetId);
            return ResponseEntity.ok(like);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при постановке лайка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при постановке лайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Убрать лайк со стикерсета
     */
    @DeleteMapping("/sticker-sets/{stickerSetId}")
    @Operation(
        summary = "Убрать лайк со стикерсета",
        description = "Удаляет лайк текущего пользователя с указанного стикерсета. " +
                     "Если лайк не найден, возвращается ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Лайк успешно убран"),
        @ApiResponse(responseCode = "400", description = "Лайк не найден"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Void> unlikeStickerSet(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("💔 Пользователь {} убирает лайк со стикерсета {}", userId, stickerSetId);
            
            likeService.unlikeStickerSet(userId, stickerSetId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при удалении лайка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при удалении лайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Переключить лайк стикерсета
     */
    @PutMapping("/sticker-sets/{stickerSetId}/toggle")
    @Operation(
        summary = "Переключить лайк стикерсета",
        description = "Переключает состояние лайка: если лайк есть - убирает его, если нет - ставит. " +
                     "Возвращает текущее состояние лайка и общее количество лайков стикерсета."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Лайк успешно переключен",
            content = @Content(schema = @Schema(implementation = LikeToggleResult.class),
                examples = @ExampleObject(value = """
                    {
                        "isLiked": true,
                        "totalLikes": 42
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<LikeToggleResult> toggleLike(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("🔄 Пользователь {} переключает лайк стикерсета {}", userId, stickerSetId);
            
            LikeToggleResult result = likeService.toggleLike(userId, stickerSetId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при переключении лайка: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при переключении лайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить лайкнутые стикерсеты текущего пользователя
     */
    @GetMapping("/sticker-sets")
    @Operation(
        summary = "Получить лайкнутые стикерсеты текущего пользователя",
        description = "Возвращает список стикерсетов, которые лайкнул текущий пользователь, " +
                     "отсортированных по дате лайка (новые сначала)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список лайкнутых стикерсетов успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "content": [
                            {
                                "id": 5,
                                "userId": 123456789,
                                "title": "Мои стикеры",
                                "name": "my_stickers_by_StickerGalleryBot",
                                "createdAt": "2025-01-15T10:30:00",
                                "categories": []
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
    public ResponseEntity<PageResponse<StickerSetDto>> getLikedStickerSets(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Код языка для локализации категорий (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("📋 Получение лайкнутых стикерсетов пользователя {}", userId);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("createdAt");
            pageRequest.setDirection("DESC");
            PageResponse<StickerSetDto> result = likeService.getLikedStickerSets(userId, pageRequest, language);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении лайкнутых стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить топ стикерсетов по лайкам
     */
    @GetMapping("/top-sticker-sets")
    @Operation(
        summary = "Получить топ стикерсетов по лайкам",
        description = "Возвращает список стикерсетов, отсортированных по количеству лайков (по убыванию). " +
                     "Включает информацию о том, лайкнул ли текущий пользователь каждый стикерсет."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Топ стикерсетов по лайкам успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "content": [
                            {
                                "stickerSet": {
                                    "id": 5,
                                    "userId": 123456789,
                                    "title": "Популярные стикеры",
                                    "name": "popular_stickers_by_StickerGalleryBot",
                                    "createdAt": "2025-01-15T10:30:00",
                                    "categories": []
                                },
                                "likesCount": 42,
                                "isLikedByCurrentUser": true
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
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetWithLikesDto>> getTopStickerSetsByLikes(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Код языка для локализации категорий (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language) {
        try {
            LOGGER.info("🏆 Получение топ стикерсетов по лайкам");
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("likesCount");
            pageRequest.setDirection("DESC");
            Long currentUserId = getCurrentUserIdOrNull();
            PageResponse<StickerSetWithLikesDto> result = likeService.getTopStickerSetsByLikes(pageRequest, language, currentUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении топа стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить все лайки текущего пользователя
     */
    @GetMapping
    @Operation(
        summary = "Получить все лайки текущего пользователя",
        description = "Возвращает список всех лайков текущего пользователя с пагинацией."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список лайков успешно получен",
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
    public ResponseEntity<PageResponse<LikeDto>> getUserLikes(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("📋 Получение лайков пользователя {}", userId);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("createdAt");
            pageRequest.setDirection("DESC");
            PageResponse<LikeDto> result = likeService.getUserLikes(userId, pageRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении лайков пользователя: {}", e.getMessage(), e);
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
        
        // Предполагаем, что principal содержит userId
        return Long.valueOf(authentication.getName());
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
