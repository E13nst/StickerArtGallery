package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.LikeDto;
import com.example.sticker_art_gallery.dto.LikeResponseDto;
import com.example.sticker_art_gallery.dto.LikeStatisticsDto;
import com.example.sticker_art_gallery.dto.LikeToggleResult;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.StickerSetWithLikesDto;
import com.example.sticker_art_gallery.service.LikeService;
import com.example.sticker_art_gallery.service.user.UserService;
import com.example.sticker_art_gallery.service.statistics.StatisticsService;
import com.example.sticker_art_gallery.model.user.UserEntity;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Контроллер для управления лайками стикерсетов
 */
@RestController
@RequestMapping("/api/likes")
@Tag(name = "Likes", description = "API для управления лайками стикерсетов")
public class LikeController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LikeController.class);
    
    private final LikeService likeService;
    private final UserService userService;
    private final StatisticsService statisticsService;
    
    public LikeController(LikeService likeService, UserService userService, StatisticsService statisticsService) {
        this.likeService = likeService;
        this.userService = userService;
        this.statisticsService = statisticsService;
    }
    
    /**
     * Поставить лайк стикерсету
     */
    @PostMapping("/stickersets/{stickerSetId}")
    @Operation(
        summary = "Поставить лайк стикерсету",
        description = "Добавляет лайк от текущего пользователя к указанному стикерсету. " +
                     "Если пользователь уже лайкнул этот стикерсет, возвращается ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Лайк успешно поставлен",
            content = @Content(schema = @Schema(implementation = LikeResponseDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "stickerSetId": 5,
                        "createdAt": "2025-01-15T10:30:00",
                        "liked": true,
                        "totalLikes": 1
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или стикерсет уже лайкнут"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<LikeResponseDto> likeStickerSet(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("❤️ Пользователь {} ставит лайк стикерсету {}", userId, stickerSetId);
            
            LikeResponseDto result = likeService.likeStickerSet(userId, stickerSetId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Пользователь не авторизован: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
    @DeleteMapping("/stickersets/{stickerSetId}")
    @Operation(
        summary = "Убрать лайк со стикерсета",
        description = "Удаляет лайк текущего пользователя с указанного стикерсета. " +
                     "Если лайк не найден, возвращается ошибка."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Лайк успешно убран",
            content = @Content(schema = @Schema(implementation = LikeResponseDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "stickerSetId": 5,
                        "createdAt": "2025-01-15T10:30:00",
                        "liked": false,
                        "totalLikes": 0
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Лайк не найден"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<LikeResponseDto> unlikeStickerSet(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.info("💔 Пользователь {} убирает лайк со стикерсета {}", userId, stickerSetId);
            
            LikeResponseDto result = likeService.unlikeStickerSet(userId, stickerSetId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Пользователь не авторизован: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
    @PutMapping("/stickersets/{stickerSetId}/toggle")
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
                        "liked": true,
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
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Пользователь не авторизован: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
    @GetMapping("/stickersets")
    @Operation(
        summary = "Получить лайкнутые стикерсеты текущего пользователя",
        description = "Возвращает список стикерсетов, которые лайкнул текущий пользователь, " +
                     "отсортированных по дате лайка (новые сначала). " +
                     "Поддерживает локализацию названий категорий через заголовок X-Language (ru/en) или автоматически из initData пользователя."
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
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId();
            LOGGER.debug("📋 Получение лайкнутых стикерсетов пользователя {} (shortInfo={})", userId, shortInfo);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("createdAt");
            pageRequest.setDirection("DESC");
            String language = getLanguageFromHeaderOrUser(request);
            PageResponse<StickerSetDto> result = likeService.getLikedStickerSets(userId, pageRequest, language, shortInfo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении лайкнутых стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить топ стикерсетов по лайкам
     */
    @GetMapping("/top-stickersets")
    @Operation(
        summary = "Получить топ стикерсетов по лайкам",
        description = "Возвращает список стикерсетов, отсортированных по количеству лайков (по убыванию). " +
                     "Поддерживает локализацию названий категорий через заголовок X-Language (ru/en) или автоматически из initData пользователя. " +
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
            HttpServletRequest request) {
        try {
            LOGGER.debug("🏆 Получение топ стикерсетов по лайкам");
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("likesCount");
            pageRequest.setDirection("DESC");
            String language = getLanguageFromHeaderOrUser(request);
            Long currentUserId = getCurrentUserIdOrNull();
            PageResponse<StickerSetWithLikesDto> result = likeService.getTopStickerSetsByLikes(pageRequest, language, currentUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении топа стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить количество лайков для стикерсета по ID (без авторизации)
     */
    @GetMapping("/stickersets/{stickerSetId}")
    @Operation(
        summary = "Получить количество лайков для стикерсета",
        description = "Возвращает количество лайков для указанного стикерсета. " +
                     "Доступен без авторизации. Если пользователь не авторизован, возвращает 'liked': false."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация о лайках успешно получена",
            content = @Content(schema = @Schema(implementation = LikeToggleResult.class),
                examples = @ExampleObject(value = """
                    {
                        "liked": false,
                        "totalLikes": 5
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<LikeToggleResult> getLikesCount(
            @Parameter(description = "Уникальный ID стикерсета", example = "5")
            @PathVariable @Positive(message = "ID стикерсета должен быть положительным числом") Long stickerSetId) {
        try {
            LOGGER.debug("📊 Получение количества лайков для стикерсета {}", stickerSetId);
            
            // Проверяем существование стикерсета
            if (!likeService.stickerSetExists(stickerSetId)) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден", stickerSetId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            long totalLikes = likeService.getLikesCount(stickerSetId);
            
            // Проверяем, авторизован ли пользователь
            Long userId = getCurrentUserIdOrNull();
            boolean liked = false;
            if (userId != null) {
                liked = likeService.isLikedByUser(userId, stickerSetId);
            }
            
            LikeToggleResult result = new LikeToggleResult(liked, totalLikes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Непредвиденная ошибка при получении количества лайков: {}", e.getMessage(), e);
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
            LOGGER.debug("📋 Получение лайков пользователя {}", userId);
            
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
    
    /**
     * Извлечь язык из заголовка X-Language или из initData пользователя
     * @param request HTTP запрос для получения заголовков
     * @return код языка (ru/en), по умолчанию "en"
     */
    private String getLanguageFromHeaderOrUser(HttpServletRequest request) {
        // Сначала проверяем заголовок X-Language
        String languageFromHeader = request.getHeader("X-Language");
        if (languageFromHeader != null && !languageFromHeader.trim().isEmpty()) {
            String lang = languageFromHeader.trim().toLowerCase();
            if ("ru".equals(lang) || "en".equals(lang)) {
                LOGGER.debug("🌐 Язык из заголовка X-Language: {}", lang);
                return lang;
            }
        }
        
        // Если заголовок не указан или некорректный, пытаемся получить из initData пользователя
        Long currentUserId = getCurrentUserIdOrNull();
        if (currentUserId != null) {
            try {
                java.util.Optional<UserEntity> userOpt = userService.findById(currentUserId);
                if (userOpt.isPresent()) {
                    String userLanguage = userOpt.get().getLanguageCode();
                    if (userLanguage != null && !userLanguage.trim().isEmpty()) {
                        String lang = userLanguage.trim().toLowerCase();
                        if ("ru".equals(lang) || "en".equals(lang)) {
                            LOGGER.debug("🌐 Язык из initData пользователя {}: {}", currentUserId, lang);
                            return lang;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("⚠️ Ошибка при получении языка пользователя {}: {}", currentUserId, e.getMessage());
            }
        }
        
        // По умолчанию возвращаем английский
        LOGGER.debug("🌐 Используется язык по умолчанию: en");
        return "en";
    }

    /**
     * Получить статистику по лайкам
     */
    @GetMapping("/statistics")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Получить статистику по лайкам",
        description = "Возвращает статистику по лайкам: общее количество, поставленные за день/неделю"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статистика получена",
            content = @Content(schema = @Schema(implementation = LikeStatisticsDto.class),
                examples = @ExampleObject(value = """
                    {
                        "total": 12345,
                        "daily": 156,
                        "weekly": 1024
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<LikeStatisticsDto> getLikeStatistics() {
        try {
            LOGGER.debug("📊 Запрос статистики по лайкам");
            LikeStatisticsDto statistics = statisticsService.getLikeStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении статистики лайков: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
