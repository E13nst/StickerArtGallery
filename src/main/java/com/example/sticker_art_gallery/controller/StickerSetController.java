package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.*;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.telegram.StickerSetCreationService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.example.sticker_art_gallery.service.ai.AutoCategorizationService;
import com.example.sticker_art_gallery.service.ai.StickerSetDescriptionService;
import com.example.sticker_art_gallery.service.StickerSetQueryService;
import com.example.sticker_art_gallery.service.statistics.StatisticsService;
import com.example.sticker_art_gallery.service.transaction.WalletService;
import com.example.sticker_art_gallery.service.swipe.SwipeTrackingService;
import com.example.sticker_art_gallery.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
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
import java.util.Set;

@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*") // Разрешаем CORS для фронтенда
@Tag(name = "Стикерсеты", description = "Управление стикерсетами пользователей")
@SecurityRequirement(name = "TelegramInitData")
public class StickerSetController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetController.class);
    private final StickerSetService stickerSetService;
    private final AutoCategorizationService autoCategorizationService;
    private final StickerSetDescriptionService stickerSetDescriptionService;
    private final StickerSetQueryService stickerSetQueryService;
    private final StatisticsService statisticsService;
    private final WalletService walletService;
    private final StickerSetControllerHelper helper;
    private final StickerSetCreationService stickerSetCreationService;
    private final TelegramBotApiService telegramBotApiService;
    private final SwipeTrackingService swipeTrackingService;
    
    @Autowired
    public StickerSetController(StickerSetService stickerSetService,
                               AutoCategorizationService autoCategorizationService,
                               StickerSetDescriptionService stickerSetDescriptionService,
                               StickerSetQueryService stickerSetQueryService,
                               StatisticsService statisticsService,
                               WalletService walletService,
                               StickerSetControllerHelper helper,
                               StickerSetCreationService stickerSetCreationService,
                               TelegramBotApiService telegramBotApiService,
                               SwipeTrackingService swipeTrackingService) {
        this.stickerSetService = stickerSetService;
        this.autoCategorizationService = autoCategorizationService;
        this.stickerSetDescriptionService = stickerSetDescriptionService;
        this.stickerSetQueryService = stickerSetQueryService;
        this.statisticsService = statisticsService;
        this.walletService = walletService;
        this.helper = helper;
        this.stickerSetCreationService = stickerSetCreationService;
        this.telegramBotApiService = telegramBotApiService;
        this.swipeTrackingService = swipeTrackingService;
    }
    
    /**
     * Получить все стикерсеты с пагинацией
     */
    @GetMapping
    @Operation(
        summary = "Получить все стикерсеты с пагинацией и фильтрацией",
        description = "Возвращает список всех стикерсетов в системе с пагинацией, фильтрацией по категориям и обогащением данных из Telegram Bot API. " +
                     "Поддерживает локализацию названий категорий через заголовок X-Language (ru/en) или автоматически из initData пользователя. " +
                     "Можно фильтровать по категориям через параметр categoryKeys. " +
                     "Можно фильтровать по пользователю через параметр userId. " +
                     "Можно показать только лайкнутые пользователем стикерсеты через параметр likedOnly=true. " +
                     "Требует авторизации через Telegram Web App."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список стикерсетов успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = {
                    @ExampleObject(name = "Обычный список стикерсетов", value = """
                        {
                            "content": [
                                {
                                    "id": 1,
                                    "userId": 123456789,
                                    "title": "Мои стикеры",
                                    "name": "my_stickers_by_StickerGalleryBot",
                                    "createdAt": "2025-09-15T10:30:00",
                                    "likesCount": 42,
                                    "isLikedByCurrentUser": true,
                                    "telegramStickerSetInfo": "{\\"name\\":\\"my_stickers_by_StickerGalleryBot\\",\\"title\\":\\"Мои стикеры\\",\\"sticker_type\\":\\"regular\\",\\"is_animated\\":false,\\"stickers\\":[...]}",
                                    "categories": [
                                        {
                                            "id": 1,
                                            "key": "animals",
                                            "name": "Животные",
                                            "description": "Стикеры с животными",
                                            "iconUrl": null,
                                            "displayOrder": 1,
                                            "isActive": true
                                        }
                                    ]
                                }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 156,
                            "totalPages": 8,
                            "first": true,
                            "last": false,
                            "hasNext": true,
                            "hasPrevious": false
                        }
                        """),
                    @ExampleObject(name = "Только лайкнутые стикерсеты (likedOnly=true)", value = """
                        {
                            "content": [
                                {
                                    "id": 5,
                                    "userId": 987654321,
                                    "title": "Лайкнутые стикеры",
                                    "name": "liked_stickers_by_StickerGalleryBot",
                                    "createdAt": "2025-01-15T10:30:00",
                                    "likesCount": 15,
                                    "isLikedByCurrentUser": true,
                                    "telegramStickerSetInfo": "{\\"name\\":\\"liked_stickers_by_StickerGalleryBot\\",\\"title\\":\\"Лайкнутые стикеры\\",\\"sticker_type\\":\\"regular\\",\\"is_animated\\":false,\\"stickers\\":[...]}",
                                    "categories": [
                                        {
                                            "id": 2,
                                            "key": "cute",
                                            "name": "Милые",
                                            "description": "Милые стикеры",
                                            "iconUrl": null,
                                            "displayOrder": 130,
                                            "isActive": true
                                        }
                                    ]
                                }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 3,
                            "totalPages": 1,
                            "first": true,
                            "last": true,
                            "hasNext": false,
                            "hasPrevious": false
                        }
                        """),
                    @ExampleObject(name = "Фильтр по автору (authorId=123456789)", value = """
                        {
                            "content": [
                                {
                                    "id": 10,
                                    "userId": 543210987,
                                    "title": "Авторский набор",
                                    "name": "author_pack_by_StickerGalleryBot",
                                    "authorId": 123456789,
                                    "createdAt": "2025-05-10T10:30:00",
                                    "likesCount": 7,
                                    "isLikedByCurrentUser": false,
                                    "categories": []
                                }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 1,
                            "totalPages": 1,
                            "first": true,
                            "last": true,
                            "hasNext": false,
                            "hasPrevious": false
                        }
                        """),
                    @ExampleObject(name = "Фильтр по пользователю (userId=123456789)", value = """
                        {
                            "content": [
                                {
                                    "id": 11,
                                    "userId": 123456789,
                                    "title": "Стикерсет пользователя",
                                    "name": "user_pack_by_StickerGalleryBot",
                                    "createdAt": "2025-05-15T10:30:00",
                                    "likesCount": 12,
                                    "isLikedByCurrentUser": false,
                                    "categories": []
                                }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 1,
                            "totalPages": 1,
                            "first": true,
                            "last": true,
                            "hasNext": false,
                            "hasPrevious": false
                        }
                        """),
                    @ExampleObject(name = "Только авторские (hasAuthorOnly=true) и официальные (officialOnly=true)", value = """
                        {
                            "content": [
                                {
                                    "id": 12,
                                    "userId": 222222222,
                                    "title": "Официальный авторский",
                                    "name": "official_author_by_StickerGalleryBot",
                                    "authorId": 111111111,
                                    "isOfficial": true,
                                    "createdAt": "2025-06-01T09:00:00",
                                    "likesCount": 24,
                                    "isLikedByCurrentUser": false,
                                    "categories": []
                                }
                            ],
                            "page": 0,
                            "size": 20,
                            "totalElements": 1,
                            "totalPages": 1,
                            "first": true,
                            "last": true,
                            "hasNext": false,
                            "hasPrevious": false
                        }
                        """)
                })),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или проблемы с Telegram Bot API")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getAllStickerSets(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Поле для сортировки (например: createdAt, likesCount)", example = "likesCount")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Направление сортировки", example = "DESC")
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction,
            @Parameter(description = "Фильтр по ключам категорий (через запятую)", example = "animals,memes")
            @RequestParam(required = false) String categoryKeys,
            @Parameter(description = "Фильтр по типу стикерсета (USER, OFFICIAL)", example = "USER")
            @RequestParam(required = false) com.example.sticker_art_gallery.model.telegram.StickerSetType type,
            @Parameter(description = "Показывать только официальные стикерсеты (устарело, используйте type=OFFICIAL)", example = "false")
            @RequestParam(defaultValue = "false") boolean officialOnly,
            @Parameter(description = "Фильтр по автору (Telegram ID)", example = "123456789")
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Показывать только авторские стикерсеты (authorId IS NOT NULL)", example = "false")
            @RequestParam(defaultValue = "false") boolean hasAuthorOnly,
            @Parameter(description = "Фильтр по пользователю (Telegram ID)", example = "123456789")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Показать только лайкнутые пользователем стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean likedOnly,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            @Parameter(description = "Режим превью: возвращать только 1 случайный стикер в telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean preview,
            HttpServletRequest request) {
        try {
            // Построение фильтра
            StickerSetFilterRequest filter = helper.buildFilter(
                page, size, sort, direction, categoryKeys, type, officialOnly,
                authorId, hasAuthorOnly, userId, likedOnly, shortInfo, preview, request
            );
            
            LOGGER.debug("📋 Получение стикерсетов: {}", filter);
            
            // Выполнение запроса через единый сервис
            PageResponse<StickerSetDto> result = stickerSetQueryService.findStickerSets(filter);
            
            LOGGER.debug("✅ Найдено {} стикерсетов на странице {} из {}", 
                    result.getContent().size(), result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
            
        } catch (UnauthorizedException e) {
            LOGGER.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить стикерсеты конкретного пользователя с фильтрацией
     * ПЕРЕМЕЩЕНО в StickerSetQueryController
     */
    // УДАЛЕНО: перенесено в StickerSetQueryController
    
    /**
     * Получить стикерсеты конкретного автора с фильтрацией
     * ПЕРЕМЕЩЕНО в StickerSetQueryController
     */
    // УДАЛЕНО: перенесено в StickerSetQueryController
    
    /**
     * Получить стикерсет по ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Получить стикерсет по ID",
        description = "Возвращает информацию о стикерсете по его уникальному идентификатору. " +
                     "Включает информацию о том, лайкнул ли текущий пользователь этот стикерсет (поле isLikedByCurrentUser). " +
                     "Для неавторизованных пользователей это поле будет false."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет найден",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "title": "Мои стикеры",
                        "name": "my_stickers_by_StickerGalleryBot",
                        "createdAt": "2025-09-15T10:30:00",
                        "likesCount": 42,
                        "isLikedByCurrentUser": true,
                        "telegramStickerSetInfo": "{\\"name\\":\\"my_stickers_by_StickerGalleryBot\\",\\"title\\":\\"Мои стикеры\\",\\"sticker_type\\":\\"regular\\",\\"is_animated\\":false,\\"stickers\\":[...]}",
                        "categories": [
                            {
                                "id": 1,
                                "key": "animals",
                                "name": "Животные",
                                "description": "Стикеры с животными",
                                "iconUrl": null,
                                "displayOrder": 1,
                                "isActive": true
                            }
                        ],
                        "isPublic": true,
                        "isBlocked": false,
                        "blockReason": null
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректный ID (должен быть положительным числом)"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> getStickerSetById(
            @Parameter(description = "Уникальный идентификатор стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo) {
        try {
            LOGGER.debug("🔍 Поиск стикерсета по ID: {} с данными Bot API (shortInfo={})", id, shortInfo);
            
            Long currentUserId = helper.getCurrentUserIdOrNull();
            LOGGER.debug("🔍 getCurrentUserIdOrNull() вернул: {}", currentUserId);
            
            // Проверяем SecurityContext для отладки
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                LOGGER.debug("🔍 SecurityContext: authenticated={}, name={}, authorities={}", 
                        auth.isAuthenticated(), auth.getName(), auth.getAuthorities());
            } else {
                LOGGER.debug("🔍 SecurityContext: authentication is null");
            }
            
            StickerSetDto dto = stickerSetService.findByIdWithBotApiData(id, null, currentUserId, shortInfo);
            
            if (dto == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            LOGGER.debug("✅ Стикерсет найден: {}", dto.getTitle());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Поиск стикерсетов по title или description
     */
    @GetMapping("/search")
    @Operation(
        summary = "Поиск стикерсетов по названию или описанию",
        description = "Ищет стикерсеты по частичному совпадению в title или description (без учёта регистра). " +
                     "Поддерживает пагинацию, фильтрацию по категориям, автору, пользователю и типу. " +
                     "Возвращает только активные и публичные стикерсеты (не заблокированные и не удалённые)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список найденных стикерсетов",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> searchStickerSets(
            @Parameter(description = "Поисковый запрос (ищет в title и description)", required = true, example = "cat")
            @RequestParam String query,
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Поле для сортировки", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Направление сортировки", example = "DESC")
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction,
            @Parameter(description = "Фильтр по ключам категорий (через запятую)", example = "animals,memes")
            @RequestParam(required = false) String categoryKeys,
            @Parameter(description = "Фильтр по типу стикерсета", example = "USER")
            @RequestParam(required = false) com.example.sticker_art_gallery.model.telegram.StickerSetType type,
            @Parameter(description = "Фильтр по автору (Telegram ID)", example = "123456789")
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Показывать только авторские стикерсеты (authorId IS NOT NULL)", example = "false")
            @RequestParam(defaultValue = "false") boolean hasAuthorOnly,
            @Parameter(description = "Фильтр по пользователю (Telegram ID)", example = "123456789")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            @Parameter(description = "Режим превью: возвращать только 1 случайный стикер в telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean preview,
            HttpServletRequest request) {
        try {
            LOGGER.debug("🔍 Поиск стикерсетов по запросу: '{}', page={}, size={}", query, page, size);
            
            Long currentUserId = helper.getCurrentUserIdOrNull();
            
            // Построение параметров запроса
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            Set<String> categoryKeysSet = null;
            if (categoryKeys != null && !categoryKeys.trim().isEmpty()) {
                categoryKeysSet = java.util.Set.of(categoryKeys.split(","));
            }
            
            String language = helper.getLanguageFromHeaderOrUser(request);
            
            // Поиск среди публичных стикерсетов
            PageResponse<StickerSetDto> result = stickerSetService.searchStickerSets(
                query,
                pageRequest,
                categoryKeysSet,
                type,
                authorId,
                hasAuthorOnly,
                userId,
                currentUserId,
                language,
                shortInfo,
                preview
            );
            
            LOGGER.debug("✅ Найдено {} стикерсетов по запросу '{}' на странице {} из {}", 
                    result.getContent().size(), query, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсетов по запросу '{}': {}", query, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить случайный стикерсет, который пользователь еще не лайкал и не дизлайкал
     */
    @GetMapping("/random")
    @Operation(
        summary = "Получить случайный стикерсет",
        description = "Возвращает случайный публичный и активный стикерсет, который пользователь еще не оценивал " +
                     "(не ставил лайк или дизлайк). Требует авторизации через Telegram Web App. " +
                     "Если все доступные стикерсеты уже оценены пользователем, возвращает 404."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Случайный стикерсет успешно получен",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 42,
                        "userId": 987654321,
                        "title": "Случайный стикерсет",
                        "name": "random_stickers_by_StickerGalleryBot",
                        "createdAt": "2025-01-10T15:30:00",
                        "likesCount": 25,
                        "dislikesCount": 3,
                        "isLikedByCurrentUser": false,
                        "isDislikedByCurrentUser": false,
                        "telegramStickerSetInfo": "{\\"name\\":\\"random_stickers_by_StickerGalleryBot\\",\\"title\\":\\"Случайный стикерсет\\",\\"sticker_type\\":\\"regular\\",\\"is_animated\\":false,\\"stickers\\":[...]}",
                        "categories": [
                            {
                                "id": 3,
                                "key": "memes",
                                "name": "Мемы",
                                "description": "Мемные стикеры",
                                "iconUrl": null,
                                "displayOrder": 50,
                                "isActive": true
                            }
                        ],
                        "isPublic": true,
                        "isBlocked": false,
                        "blockReason": null
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "404", description = "Нет доступных стикерсетов, которые пользователь еще не оценил"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> getRandomStickerSet(
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            HttpServletRequest request) {
        try {
            Long currentUserId = helper.getCurrentUserIdOrNull();
            
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Попытка получить случайный стикерсет без авторизации");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            LOGGER.debug("🎲 Получение случайного стикерсета для пользователя {} (shortInfo={})", currentUserId, shortInfo);
            
            // Проверяем лимит свайпов перед возвратом случайного стикерсета
            try {
                swipeTrackingService.checkDailyLimit(currentUserId);
            } catch (com.example.sticker_art_gallery.exception.SwipeLimitExceededException e) {
                LOGGER.warn("⚠️ Достигнут лимит свайпов для пользователя {}: {}", currentUserId, e.getMessage());
                throw e; // Пробрасываем исключение для обработки в exception handler
            }
            
            String language = helper.getLanguageFromHeaderOrUser(request);
            StickerSetDto randomStickerSet = stickerSetService.findRandomStickerSetNotRatedByUser(
                    currentUserId, language, shortInfo);
            
            if (randomStickerSet == null) {
                LOGGER.debug("⚠️ Для пользователя {} не найдено стикерсетов, которые он еще не оценивал", currentUserId);
                return ResponseEntity.notFound().build();
            }
            
            LOGGER.debug("✅ Найден случайный стикерсет: {} (id={})", randomStickerSet.getTitle(), randomStickerSet.getId());
            return ResponseEntity.ok(randomStickerSet);
            
        } catch (com.example.sticker_art_gallery.exception.SwipeLimitExceededException e) {
            // Исключение обрабатывается в ValidationExceptionHandler и возвращает 429
            throw e;
        } catch (UnauthorizedException e) {
            LOGGER.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении случайного стикерсета: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить батч случайных стикерсетов, которые пользователь еще не лайкал и не дизлайкал
     */
    @GetMapping("/random/batch")
    @Operation(
        summary = "Получить батч случайных стикерсетов",
        description = "Возвращает страницу случайных публичных и активных стикерсетов, которые пользователь еще не оценивал " +
                     "(не ставил лайк или дизлайк). Поддерживает пагинацию. Требует авторизации через Telegram Web App. " +
                     "Если все доступные стикерсеты уже оценены пользователем, возвращает пустую страницу."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Батч случайных стикерсетов успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "429", description = "Достигнут дневной лимит свайпов"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getRandomStickerSetsBatch(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Поле для сортировки (игнорируется, используется RANDOM())", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Направление сортировки (игнорируется, используется RANDOM())", example = "DESC")
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            @Parameter(description = "Режим превью: возвращать только 1 случайный стикер в telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean preview,
            HttpServletRequest request) {
        try {
            Long currentUserId = helper.getCurrentUserIdOrNull();
            
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Попытка получить батч случайных стикерсетов без авторизации");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            LOGGER.debug("🎲 Получение батча случайных стикерсетов для пользователя {}: page={}, size={}, shortInfo={}, preview={}", 
                    currentUserId, page, size, shortInfo, preview);
            
            // Проверяем лимит свайпов перед возвратом случайных стикерсетов
            try {
                swipeTrackingService.checkDailyLimit(currentUserId);
            } catch (com.example.sticker_art_gallery.exception.SwipeLimitExceededException e) {
                LOGGER.warn("⚠️ Достигнут лимит свайпов для пользователя {}: {}", currentUserId, e.getMessage());
                throw e; // Пробрасываем исключение для обработки в exception handler
            }
            
            // Построение параметров запроса
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            String language = helper.getLanguageFromHeaderOrUser(request);
            PageResponse<StickerSetDto> result = stickerSetService.findRandomStickerSetsNotRatedByUser(
                    currentUserId, pageRequest, language, shortInfo, preview);
            
            LOGGER.debug("✅ Найдено {} случайных стикерсетов для пользователя {} на странице {} из {}", 
                    result.getContent().size(), currentUserId, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
            
        } catch (com.example.sticker_art_gallery.exception.SwipeLimitExceededException e) {
            // Исключение обрабатывается в ValidationExceptionHandler и возвращает 429
            throw e;
        } catch (UnauthorizedException e) {
            LOGGER.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении батча случайных стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Создать новый стикерсет
     */
    @PostMapping
    @Operation(
        summary = "Создать новый стикерсет",
        description = """
            Регистрирует в галерее уже существующий набор стикеров Telegram.
            
            **Формат запроса**
            ```
            POST /api/stickersets
            Content-Type: application/json
            Headers: X-Telegram-Init-Data: <initData>
            
            {
              "name": "https://t.me/addstickers/my_pack_by_bot",
              "title": "Мои стикеры",
              "categoryKeys": ["animals", "cute"],
              "visibility": "PUBLIC"
            }
            ```
            
            Поле `name` обязательно. Остальные поля опциональны: `title` подтягивается из Telegram Bot API, если не указано;
            `visibility` по умолчанию `PUBLIC` (если не указаны ни `visibility`, ни `isPublic`). 
            Пользователь определяется по заголовку `X-Telegram-Init-Data`.
            
            **Параметры видимости**
            - `visibility`: "PUBLIC" (виден всем в галерее) или "PRIVATE" (виден только владельцу)
            - `isPublic`: устаревшее поле, поддерживается для обратной совместимости. Используйте `visibility` вместо него.
            - Если указаны оба поля, приоритет у `visibility`.
            - Если не указаны ни `visibility`, ни `isPublic`, используется `PUBLIC` по умолчанию.
            
            **Результат**
            Возвращает полный `StickerSetDto`, идентичный ответу `GET /api/stickersets/{id}` (включая категории, счётчики и данные Telegram Bot API).
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Стикерсет успешно создан",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 5,
                        "userId": 123456789,
                        "title": "Мои стикеры",
                        "name": "my_stickers_by_StickerGalleryBot",
                        "createdAt": "2025-01-15T14:30:00",
                        "telegramStickerSetInfo": "{\\"name\\":\\"my_stickers_by_StickerGalleryBot\\",\\"title\\":\\"Мои стикеры\\",\\"sticker_type\\":\\"regular\\",\\"is_animated\\":false,\\"stickers\\":[...]}",
                        "categories": [
                            {
                                "id": 1,
                                "key": "animals",
                                "name": "Animals",
                                "description": "Stickers with animals",
                                "iconUrl": null,
                                "displayOrder": 1,
                                "isActive": true
                            },
                            {
                                "id": 2,
                                "key": "cute",
                                "name": "Cute",
                                "description": "Cute and adorable stickers",
                                "iconUrl": null,
                                "displayOrder": 130,
                                "isActive": true
                            }
                        ]
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации данных",
            content = @Content(examples = {
                @ExampleObject(name = "Дубликат имени", value = """
                    {
                        "error": "Ошибка валидации",
                        "message": "Стикерсет с именем 'existing_sticker_set' уже существует в галерее"
                    }
                    """),
                @ExampleObject(name = "Некорректное имя", value = """
                    {
                        "error": "Ошибка валидации",
                        "message": "Некорректное имя стикерсета или URL. Ожидается имя стикерсета или URL вида https://t.me/addstickers/имя_стикерсета"
                    }
                    """),
                @ExampleObject(name = "Несуществующие категории", value = """
                    {
                        "error": "Ошибка валидации",
                        "message": "Категории с ключами [non_existent_category] не найдены"
                    }
                    """)
            })),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "error": "Unauthorized",
                    "message": "Требуется авторизация через Telegram Web App"
                }
                """))),
        @ApiResponse(responseCode = "403", description = "Пользователь заблокирован",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "error": "Forbidden",
                    "message": "User is blocked"
                }
                """))),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден в Telegram",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "error": "Ошибка валидации",
                    "message": "Стикерсет 'nonexistent_sticker_set' не найден в Telegram"
                }
                """))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "error": "Внутренняя ошибка сервера",
                    "message": "Произошла непредвиденная ошибка при создании стикерсета"
                }
                """)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(
            schema = @Schema(implementation = CreateStickerSetDto.class),
            examples = {
                @ExampleObject(name = "С visibility (рекомендуется)", value = """
                    {
                      "name": "https://t.me/addstickers/my_pack_by_bot",
                      "title": "Мои стикеры",
                      "categoryKeys": ["animals", "cute"],
                      "visibility": "PUBLIC"
                    }
                    """),
                @ExampleObject(name = "С isPublic (устарело, для обратной совместимости)", value = """
                    {
                      "name": "https://t.me/addstickers/my_pack_by_bot",
                      "title": "Мои стикеры",
                      "categoryKeys": ["animals", "cute"],
                      "isPublic": true
                    }
                    """)
            }
        )
    )
    public ResponseEntity<?> createStickerSet(
            @Valid @RequestBody CreateStickerSetDto createDto,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            HttpServletRequest request) {
        String language = helper.getLanguageFromHeaderOrUser(request);
        try {
            LOGGER.info("➕ Создание нового стикерсета: {} (shortInfo={})", createDto.getName(), shortInfo);
            // Visibility устанавливается в сервисе по умолчанию

            Long currentUserId = helper.getCurrentUserIdOrNull();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of(
                                "error", helper.languageResponse(language, "Требуется авторизация", "Unauthorized"),
                                "message", helper.languageResponse(language, "Пользователь не авторизован", "User is not authenticated")
                        ));
            }

            StickerSet newStickerSet = stickerSetService.createStickerSet(createDto, language);
            String responseLanguage = (language == null || language.isBlank()) ? "en" : language;
            StickerSetDto createdDto = stickerSetService.findByIdWithBotApiData(newStickerSet.getId(), responseLanguage, currentUserId, shortInfo);
            if (createdDto == null) {
                createdDto = StickerSetDto.fromEntity(newStickerSet, responseLanguage, currentUserId);
            }
            
            LOGGER.info("✅ Стикерсет создан с ID: {} (title: '{}', userId: {})",
                       newStickerSet.getId(), createdDto.getTitle(), createdDto.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при создании стикерсета: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(java.util.Map.of(
                    "error", helper.languageResponse(language, "Ошибка валидации", "Validation error"),
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании стикерсета", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "error", "Внутренняя ошибка сервера",
                    "message", "Произошла непредвиденная ошибка при создании стикерсета"
                ));
        }
    }
    
    /**
     * Удалить стикерсет
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить стикерсет",
        description = "Удаляет стикерсет по его ID. Администратор может удалять любые стикерсеты, обычный пользователь - только свои."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Стикерсет успешно удален"),
        @ApiResponse(responseCode = "400", description = "Некорректный ID (должен быть положительным числом)"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно удалять только свои стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Void> deleteStickerSet(
            @Parameter(description = "ID стикерсета для удаления", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("🗑️ Удаление стикерсета с ID: {}", id);
            
            StickerSet existingStickerSet = stickerSetService.findById(id);
            if (existingStickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден для удаления", id);
                return ResponseEntity.notFound().build();
            }
            
            // Проверяем права доступа
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                Long currentUserId = Long.valueOf(authentication.getName());
                
                // Проверяем: админ или владелец стикерсета
                boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                boolean isOwner = existingStickerSet.getUserId() != null && existingStickerSet.getUserId().equals(currentUserId);
                
                if (!isAdmin && !isOwner) {
                    LOGGER.warn("⚠️ Пользователь {} попытался удалить чужой стикерсет {}", currentUserId, id);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
                
                LOGGER.debug("✅ Проверка прав пройдена: isAdmin={}, isOwner={}", isAdmin, isOwner);
            }
            
            stickerSetService.deleteById(id);
            LOGGER.info("✅ Стикерсет с ID {} удален", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить категории стикерсета
     */
    @PutMapping("/{id}/categories")
    @Operation(
        summary = "Обновить категории стикерсета",
        description = "Обновляет категории существующего стикерсета. Полностью заменяет текущие категории на новые. " +
                     "Передайте пустой массив, чтобы удалить все категории. " +
                     "Все ключи категорий должны существовать в системе. " +
                     "Администратор может обновлять любые стикерсеты, обычный пользователь - только свои."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Категории стикерсета успешно обновлены",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "title": "Мои стикеры",
                        "name": "my_stickers_by_StickerGalleryBot",
                        "createdAt": "2025-09-15T10:30:00",
                        "categories": [
                            {
                                "id": 1,
                                "key": "animals",
                                "name": "Животные",
                                "description": "Стикеры с животными"
                            },
                            {
                                "id": 2,
                                "key": "cute",
                                "name": "Милые",
                                "description": "Милые стикеры"
                            }
                        ]
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или несуществующие категории"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно обновлять только свои стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> updateStickerSetCategories(
            @Parameter(description = "ID стикерсета для обновления категорий", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Список ключей категорий", required = true)
            @RequestBody java.util.Set<String> categoryKeys,
            HttpServletRequest request) {
        try {
            LOGGER.info("🏷️ Обновление категорий стикерсета с ID: {}, категории: {}", id, categoryKeys);
            
            // Получаем стикерсет для проверки прав доступа
            StickerSet existingStickerSet = stickerSetService.findById(id);
            if (existingStickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден для обновления категорий", id);
                return ResponseEntity.notFound().build();
            }
            
            // Проверяем права доступа
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                Long currentUserId = Long.valueOf(authentication.getName());
                
                // Проверяем: админ или владелец стикерсета
                boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                boolean isOwner = existingStickerSet.getUserId() != null && existingStickerSet.getUserId().equals(currentUserId);
                
                if (!isAdmin && !isOwner) {
                    LOGGER.warn("⚠️ Пользователь {} попытался обновить категории чужого стикерсета {}", currentUserId, id);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
                
                LOGGER.debug("✅ Проверка прав пройдена: isAdmin={}, isOwner={}", isAdmin, isOwner);
            }
            
            StickerSet updatedStickerSet = stickerSetService.updateCategories(id, categoryKeys);
            
            LOGGER.info("✅ Категории стикерсета {} успешно обновлены", id);
            String language = helper.getLanguageFromHeaderOrUser(request);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updatedStickerSet, language));
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректные данные для обновления категорий стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении категорий стикерсета {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Предложить категории для стикерсета (предпросмотр или применение)
     */
    
    /**
     * Сгенерировать описание стикерсета с помощью AI
     */
    
    /**
     * Опубликовать стикерсет в галерее (сделать публичным)
     */
    
    /**
     * Скрыть стикерсет из галереи (сделать приватным)
     */
    
    /**
     * Внутренний метод для изменения видимости стикерсета
     */
    
    /**
     * Заблокировать стикерсет (только для админа)
     */
    
    /**
     * Разблокировать стикерсет (только для админа)
     */
    
    /**
     * Отметить стикерсет как официальный (только для админа)
     */
    
    /**
     * Снять признак официального стикерсета (только для админа)
     */

    /**
     * Установить автора стикерсета (только для админа)
     */

    // ============================================================================
    // Новые эндпоинты для создания и управления стикерсетами через Telegram Bot API
    // ============================================================================

    /**
     * Создает новый стикерсет в Telegram с первым стикером и регистрирует его в БД
     */
    @PostMapping("/create")
    @Operation(
        summary = "Создать новый стикерсет с первым стикером",
        description = """
            Создает новый стикерсет в Telegram через Bot API и регистрирует его в БД.
            Если имя стикерсета не указано, генерируется автоматически: {username}_by_{botUsername} или user_{userId}_by_{botUsername}.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет успешно создан"),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные"),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> createStickerSetWithSticker(
            @Valid @RequestBody CreateStickerSetWithStickerDto createDto,
            HttpServletRequest request) {
        try {
            Long userId = helper.getCurrentUserId();
            String language = helper.getLanguageFromHeaderOrUser(request);
            
            LOGGER.info("🎯 Создание стикерсета с первым стикером: userId={}, imageUuid={}", 
                    userId, createDto.getImageUuid());
            
            StickerSet stickerSet = stickerSetCreationService.createWithSticker(
                userId,
                createDto.getImageUuid(),
                createDto.getTitle(),
                createDto.getName(),
                createDto.getEmoji(),
                createDto.getCategoryKeys(),
                createDto.getVisibility()
            );
            
            if (stickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет создан в Telegram, но не зарегистрирован в БД");
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(null); // 202 Accepted - создан в Telegram, но не в БД
            }
            
            StickerSetDto dto = stickerSetService.findByIdWithBotApiData(
                stickerSet.getId(), language, userId, false
            );
            
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при создании стикерсета: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании стикерсета: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Добавляет стикер в существующий стикерсет
     */
    @PostMapping("/{id}/stickers")
    @Operation(
        summary = "Добавить стикер в стикерсет",
        description = """
            Добавляет стикер в существующий стикерсет.
            Проверяет права доступа (только владелец) и лимит 120 стикеров.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикер успешно добавлен"),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные или стикерсет полон"),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> addStickerToSet(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive Long id,
            @Valid @RequestBody AddStickerDto addDto) {
        try {
            Long userId = helper.getCurrentUserId();
            
            // Проверка прав доступа
            StickerSet stickerSet = stickerSetService.findById(id);
            if (stickerSet == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!helper.isOwnerOrAdmin(stickerSet.getUserId(), userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            LOGGER.info("➕ Добавление стикера в стикерсет: id={}, userId={}, imageUuid={}", 
                    id, userId, addDto.getImageUuid());
            
            stickerSetCreationService.saveImageToStickerSet(
                userId,
                addDto.getImageUuid(),
                stickerSet.getName(),
                addDto.getEmoji()
            );
            
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Ошибка при добавлении стикера: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при добавлении стикера: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Удаляет стикер из стикерсета
     */
    @DeleteMapping("/{id}/stickers/{stickerFileId}")
    @Operation(
        summary = "Удалить стикер из стикерсета",
        description = "Удаляет стикер из стикерсета по file_id. Проверяет права доступа (только владелец)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикер успешно удален"),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> deleteStickerFromSet(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive Long id,
            @Parameter(description = "file_id стикера в Telegram", required = true)
            @PathVariable String stickerFileId) {
        try {
            Long userId = helper.getCurrentUserId();
            
            // Проверка прав доступа
            StickerSet stickerSet = stickerSetService.findById(id);
            if (stickerSet == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!helper.isOwnerOrAdmin(stickerSet.getUserId(), userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            LOGGER.info("🗑️ Удаление стикера из стикерсета: id={}, userId={}, fileId={}", 
                    id, userId, stickerFileId);
            
            boolean success = telegramBotApiService.deleteStickerFromSet(userId, stickerFileId);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete sticker from Telegram"));
            }
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении стикера: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Универсальный метод: сохраняет изображение из /data/images в стикерсет
     */
    @PostMapping("/save-image")
    @Operation(
        summary = "Сохранить изображение в стикерсет",
        description = """
            Универсальный метод для сохранения любого изображения из /data/images в стикерсет.
            Если stickerSetName не указан, используется дефолтный стикерсет пользователя.
            Если указан stickerSetName, проверяется что имя заканчивается на _by_{botUsername}.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Изображение успешно сохранено. Возвращает объект SaveImageToStickerSetResponseDto с полями: stickerSetName (имя стикерсета), stickerIndex (индекс стикера в стикерсете, 0-based), stickerFileId (Telegram file_id стикера), title (название стикерсета)",
            content = @Content(schema = @Schema(implementation = SaveImageToStickerSetResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные или стикерсет полон"),
        @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> saveImageToStickerSet(
            @Valid @RequestBody SaveImageToStickerSetDto saveDto) {
        try {
            Long userId = helper.getCurrentUserId();
            
            LOGGER.info("💾 Сохранение изображения в стикерсет: userId={}, imageUuid={}, stickerSetName={}", 
                    userId, saveDto.getImageUuid(), saveDto.getStickerSetName());
            
            SaveImageToStickerSetResponseDto result = stickerSetCreationService.saveImageToStickerSet(
                userId,
                saveDto.getImageUuid(),
                saveDto.getStickerSetName(),
                saveDto.getEmoji()
            );
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            LOGGER.warn("⚠️ Ошибка при сохранении: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при сохранении изображения: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Очистить автора стикерсета (только для админа)
     */
    

    /**
     * Получить статистику по стикерсетам
     */
} 