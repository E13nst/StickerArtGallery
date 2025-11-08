package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.*;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.LikeService;
import com.example.sticker_art_gallery.service.user.UserService;
import com.example.sticker_art_gallery.service.ai.AutoCategorizationService;
import com.example.sticker_art_gallery.model.user.UserEntity;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*") // Разрешаем CORS для фронтенда
@Tag(name = "Стикерсеты", description = "Управление стикерсетами пользователей")
@SecurityRequirement(name = "TelegramInitData")
public class StickerSetController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetController.class);
    private final StickerSetService stickerSetService;
    private final LikeService likeService;
    private final UserService userService;
    private final AutoCategorizationService autoCategorizationService;
    
    @Autowired
    public StickerSetController(StickerSetService stickerSetService, LikeService likeService, 
                               UserService userService, AutoCategorizationService autoCategorizationService) {
        this.stickerSetService = stickerSetService;
        this.likeService = likeService;
        this.userService = userService;
        this.autoCategorizationService = autoCategorizationService;
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
            @Parameter(description = "Показывать только официальные стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean officialOnly,
            @Parameter(description = "Фильтр по автору (Telegram ID)", example = "123456789")
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Показывать только авторские стикерсеты (authorId IS NOT NULL)", example = "false")
            @RequestParam(defaultValue = "false") boolean hasAuthorOnly,
            @Parameter(description = "Показать только лайкнутые пользователем стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean likedOnly,
            HttpServletRequest request) {
        try {
            LOGGER.info("📋 Получение всех стикерсетов с пагинацией: page={}, size={}, sort={}, direction={}, categoryKeys={}, officialOnly={}, authorId={}, hasAuthorOnly={}, likedOnly={}", 
                    page, size, sort, direction, categoryKeys, officialOnly, authorId, hasAuthorOnly, likedOnly);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            String language = getLanguageFromHeaderOrUser(request);
            PageResponse<StickerSetDto> result;
            Long currentUserId = getCurrentUserIdOrNull();
            
            if (likedOnly) {
                // Фильтрация по лайкнутым стикерсетам
                if (currentUserId == null) {
                    LOGGER.warn("⚠️ Запрос лайкнутых стикерсетов от неавторизованного пользователя");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                
                // Проверяем, есть ли дополнительная фильтрация по категориям
                if (categoryKeys != null && !categoryKeys.trim().isEmpty()) {
                    // Комбинированная фильтрация: лайкнутые + категории
                    String[] categoryKeyArray = categoryKeys.split(",");
                    result = likeService.getLikedStickerSetsByCategories(currentUserId, categoryKeyArray, pageRequest, language);
                } else {
                    // Только лайкнутые стикерсеты
                    result = likeService.getLikedStickerSets(currentUserId, pageRequest, language);
                }
            } else if (categoryKeys != null && !categoryKeys.trim().isEmpty()) {
                // Фильтрация только по категориям (без лайков)
                String[] categoryKeyArray = categoryKeys.split(",");
                result = stickerSetService.findByCategoryKeys(categoryKeyArray, pageRequest, language, currentUserId, officialOnly, authorId, hasAuthorOnly);
            } else {
                // Без фильтрации
                result = stickerSetService.findAllWithPagination(pageRequest, language, currentUserId, officialOnly, authorId, hasAuthorOnly);
            }
            
            LOGGER.debug("✅ Найдено {} стикерсетов на странице {} из {}", 
                    result.getContent().size(), result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении всех стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
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
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("🔍 Поиск стикерсета по ID: {} с данными Bot API", id);
            
            Long currentUserId = getCurrentUserIdOrNull();
            StickerSetDto dto = stickerSetService.findByIdWithBotApiData(id, currentUserId);
            
            if (dto == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            LOGGER.info("✅ Стикерсет найден: {}", dto.getTitle());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить стикерсеты пользователя с пагинацией
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Получить стикерсеты пользователя с пагинацией",
        description = "Возвращает стикерсеты, созданные конкретным пользователем, с пагинацией и обогащением данных из Telegram Bot API. " +
                      "Поддерживает фильтры по категориям, наличию автора и лайкам текущего пользователя. " +
                      "Если запрос выполняет владелец (userId совпадает с авторизованным пользователем из initData) или администратор, " +
                      "возвращаются все его стикерсеты. Для остальных пользователей возвращаются только публичные (isPublic = true)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список стикерсетов пользователя получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "content": [
                            {
                                "id": 1,
                                "userId": 123456789,
                                "title": "Мои стикеры",
                                "name": "my_stickers_by_StickerGalleryBot",
                                "createdAt": "2025-09-15T10:30:00",
                                "telegramStickerSetInfo": "{\\"name\\":\\"my_stickers_by_StickerGalleryBot\\",\\"title\\":\\"Мои стикеры\\",\\"sticker_type\\":\\"regular\\",\\"is_animated\\":false,\\"stickers\\":[...]}"
                            }
                        ],
                        "page": 0,
                        "size": 20,
                        "totalElements": 5,
                        "totalPages": 1,
                        "first": true,
                        "last": true,
                        "hasNext": false,
                        "hasPrevious": false
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или проблемы с Telegram Bot API")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getStickerSetsByUserId(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable @Positive(message = "ID пользователя должен быть положительным числом") Long userId,
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Поле для сортировки", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Направление сортировки", example = "DESC")
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction,
            @Parameter(description = "Фильтр по ключам категорий (через запятую)", example = "animals,cute")
            @RequestParam(required = false) String categoryKeys,
            @Parameter(description = "Показывать только авторские стикерсеты (authorId IS NOT NULL)", example = "false")
            @RequestParam(defaultValue = "false") boolean hasAuthorOnly,
            @Parameter(description = "Показать только лайкнутые текущим пользователем стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean likedOnly) {
        try {
            LOGGER.info("🔍 Поиск стикерсетов для пользователя: {} с пагинацией: page={}, size={}, sort={}, direction={}, categoryKeys={}, hasAuthorOnly={}, likedOnly={}", 
                    userId, page, size, sort, direction, categoryKeys, hasAuthorOnly, likedOnly);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);

            Long currentUserId = getCurrentUserIdOrNull();
            boolean includePrivate = (currentUserId != null && currentUserId.equals(userId)) || isAdmin();
            if (likedOnly && currentUserId == null) {
                LOGGER.warn("⚠️ Запрос likedOnly=true для стикерсетов пользователя {} от неавторизованного клиента", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Set<String> categoryKeySet = parseCategoryKeys(categoryKeys);

            PageResponse<StickerSetDto> result = stickerSetService.findByUserIdWithPagination(
                    userId,
                    pageRequest,
                    categoryKeySet,
                    hasAuthorOnly,
                    likedOnly,
                    currentUserId,
                    includePrivate
            );
            
            LOGGER.debug("✅ Найдено {} стикерсетов для пользователя {} на странице {} из {}", 
                    result.getContent().size(), userId, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсетов для пользователя: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить авторские стикерсеты
     */
    @GetMapping("/author/{authorId}")
    @Operation(
        summary = "Получить авторские стикерсеты",
        description = "Возвращает стикерсеты, у которых указан переданный authorId. " +
                      "Если запрос выполняет владелец (authorId совпадает с авторизованным пользователем из initData) или администратор, " +
                      "возвращаются все его стикерсеты (включая приватные). Иначе возвращаются только публичные (isPublic = true)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список авторских стикерсетов получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или проблемы с Telegram Bot API")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getStickerSetsByAuthorId(
            @Parameter(description = "Telegram ID автора", required = true, example = "123456789")
            @PathVariable @Positive(message = "ID автора должен быть положительным числом") Long authorId,
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Поле для сортировки", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Направление сортировки", example = "DESC")
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction,
            @Parameter(description = "Фильтр по ключам категорий (через запятую)", example = "animals,cute")
            @RequestParam(required = false) String categoryKeys) {
        try {
            LOGGER.info("🔍 Поиск авторских стикерсетов: authorId={}, page={}, size={}, sort={}, direction={}, categoryKeys={}",
                    authorId, page, size, sort, direction, categoryKeys);

            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);

            Long currentUserId = getCurrentUserIdOrNull();
            boolean includePrivate = (currentUserId != null && currentUserId.equals(authorId)) || isAdmin();

            Set<String> categoryKeySet = parseCategoryKeys(categoryKeys);

            PageResponse<StickerSetDto> result = stickerSetService.findByAuthorIdWithPagination(
                    authorId,
                    pageRequest,
                    categoryKeySet,
                    currentUserId,
                    includePrivate
            );

            LOGGER.debug("✅ Найдено {} авторских стикерсетов для authorId {} на странице {} из {}",
                    result.getContent().size(), authorId, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске авторских стикерсетов для автора: {}", authorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Разобрать строку ключей категорий в множество (с сохранением порядка)
     */
    private Set<String> parseCategoryKeys(String categoryKeys) {
        if (categoryKeys == null || categoryKeys.trim().isEmpty()) {
            return null;
        }
        Set<String> result = Arrays.stream(categoryKeys.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return result.isEmpty() ? null : result;
    }
    
    /**
     * Получить стикерсет по названию
     */
    @GetMapping("/search")
    @Operation(
        summary = "Поиск стикерсета по названию",
        description = "Ищет стикерсет по его уникальному имени (name). Имя используется в Telegram API."
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
                        "createdAt": "2025-09-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректное название (не может быть пустым)"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным названием не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> getStickerSetByName(
            @Parameter(description = "Уникальное имя стикерсета для Telegram API", required = true, example = "my_stickers_by_StickerGalleryBot")
            @RequestParam @NotBlank(message = "Название не может быть пустым") String name) {
        try {
            LOGGER.info("🔍 Поиск стикерсета по названию: {} с данными Bot API", name);
            StickerSetDto dto = stickerSetService.findByNameWithBotApiData(name);
            
            if (dto == null) {
                LOGGER.warn("⚠️ Стикерсет с названием '{}' не найден", name);
                return ResponseEntity.notFound().build();
            }
            
            LOGGER.info("✅ Стикерсет найден: {}", dto.getTitle());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсета с названием: {}", name, e);
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
            
            **Запрос**
            - Метод: `POST /api/stickersets`
            - Тело: отсутствует (используются только query-параметры)
            
            **Обязательные query-параметры**
            - `name` — имя стикерсета в Telegram (например, `my_pack_by_bot`) или полный URL `https://t.me/addstickers/<name>`.
              Значение нормализуется: обрезаются пробелы, из URL извлекается имя, затем оно приводится к нижнему регистру.
            
            **Опциональные query-параметры**
            - `title` — человекочитаемое название (до 64 символов). Если не указано, подтягивается из Telegram Bot API.
            - `categoryKeys` — список ключей категорий (указывайте несколько параметров `categoryKeys=animals&categoryKeys=cute`).
            - `isPublic` — публиковать ли набор в галерее (по умолчанию `true`).
            
            **Автоматически определяемые значения**
            - `userId` — берется из заголовка `X-Telegram-Init-Data` после успешной аутентификации Telegram Web App.
            - `title` — извлекается из Telegram Bot API, если не передан явно.
            
            **Алгоритм работы**
            1. Валидируется заголовок `X-Telegram-Init-Data`; заблокированные пользователи получают 403 до выполнения бизнес-логики.
            2. Имя стикерсета нормализуется и проверяется на уникальность в локальной базе.
            3. Выполняется запрос к Telegram Bot API, чтобы убедиться, что набор существует, и получить его метаданные.
            4. По ключам `categoryKeys` подтягиваются существующие категории (если указаны).
            5. Создается запись `StickerSet` с заполнением всех служебных полей (`userId`, `title`, `isPublic`, `likesCount`, `createdAt` и т.д.).
            
            **Ответ (`StickerSetDto`)**
            - `id`, `userId`, `name`, `title`, `createdAt`, `likesCount`, `isPublic`, `isBlocked`, `blockReason`, `isOfficial`,
              `authorId`, `categories`, `telegramStickerSetInfo`, `isLikedByCurrentUser`.
            
            **Необходимые заголовки**
            - `X-Telegram-Init-Data` — подписанные данные Telegram Web App, содержащие пользователя и подпись.
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
    @Parameters({
        @Parameter(
            name = "name",
            in = ParameterIn.QUERY,
            required = true,
            description = "Имя стикерсета в Telegram или полный URL `https://t.me/addstickers/<name>`. Значение нормализуется: из URL извлекается имя и приводится к нижнему регистру.",
            example = "my_pack_by_bot"
        ),
        @Parameter(
            name = "title",
            in = ParameterIn.QUERY,
            description = "Произвольное название до 64 символов. Если не указано, берётся из Telegram Bot API.",
            example = ""
        ),
        @Parameter(
            name = "categoryKeys",
            in = ParameterIn.QUERY,
            description = "Повторяющийся параметр для привязки категорий. Укажите несколько раз: `categoryKeys=animals&categoryKeys=cute`.",
            array = @ArraySchema(schema = @Schema(type = "string")),
            example = "animals"
        ),
        @Parameter(
            name = "isPublic",
            in = ParameterIn.QUERY,
            description = "Публиковать ли набор в галерее. По умолчанию `true`.",
            schema = @Schema(type = "boolean", defaultValue = "true"),
            example = "true"
        )
    })
    @RequestBody(required = false, content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<?> createStickerSet(
            @Valid @ModelAttribute CreateStickerSetDto createDto,
            HttpServletRequest request) {
        String language = getLanguageFromHeaderOrUser(request);
        try {
            LOGGER.info("➕ Создание нового стикерсета: {}", createDto.getName());
            if (createDto.getIsPublic() == null) {
                createDto.setIsPublic(true);
            }

            Long currentUserId = getCurrentUserIdOrNull();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of(
                                "error", languageResponse(language, "Требуется авторизация", "Unauthorized"),
                                "message", languageResponse(language, "Пользователь не авторизован", "User is not authenticated")
                        ));
            }

            StickerSet newStickerSet = stickerSetService.createStickerSet(createDto, language);
            StickerSetDto createdDto = StickerSetDto.fromEntity(newStickerSet);
            
            LOGGER.info("✅ Стикерсет создан с ID: {} (title: '{}', userId: {})", 
                       createdDto.getId(), createdDto.getTitle(), createdDto.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при создании стикерсета: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(java.util.Map.of(
                    "error", languageResponse(language, "Ошибка валидации", "Validation error"),
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
            
            StickerSet updatedStickerSet = stickerSetService.updateCategories(id, categoryKeys);
            
            LOGGER.info("✅ Категории стикерсета {} успешно обновлены", id);
            String language = getLanguageFromHeaderOrUser(request);
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
    @PostMapping("/{id}/ai/suggest-categories")
    @Operation(
        summary = "Предложить категории для стикерсета",
        description = "Использует AI (ChatGPT) для анализа title стикерсета и предложения наиболее подходящих категорий. " +
                     "С параметром apply=false возвращает только предпросмотр, с apply=true - применяет категории. " +
                     "Параметр minConfidence задает минимальный уровень уверенности (0.0-1.0) для применения категорий " +
                     "при apply=true. При apply=false этот параметр не имеет значения. " +
                     "Доступно владельцу стикерсета или администратору. " +
                     "Для работы требуется переменная окружения OPENAI_API_KEY."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Категории успешно предложены",
            content = @Content(schema = @Schema(implementation = CategorySuggestionResult.class),
                examples = @ExampleObject(value = """
                    {
                        "analyzedTitle": "Cute Cats",
                        "suggestedCategories": [
                            {
                                "categoryKey": "animals",
                                "categoryName": "Животные",
                                "confidence": 0.95,
                                "reason": "Contains cat-related imagery"
                            },
                            {
                                "categoryKey": "cute",
                                "categoryName": "Милые",
                                "confidence": 0.87,
                                "reason": "Title explicitly mentions cute theme"
                            }
                        ]
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или стикерсет без title"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно категоризовать только свои стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или ошибка при работе с AI")
    })
    public ResponseEntity<CategorySuggestionResult> suggestCategoriesForStickerSet(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Применить категории (true) или только предпросмотр (false)", example = "false")
            @RequestParam(defaultValue = "false") boolean apply,
            @Parameter(description = "Минимальный уровень уверенности (0.0-1.0) для применения категорий при apply=true. " +
                                    "Категории с confidence ниже этого значения не будут применены. " +
                                    "При apply=false этот параметр не имеет значения.", example = "0.8")
            @RequestParam(required = false) 
            @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "minConfidence должен быть >= 0.0")
            @jakarta.validation.constraints.DecimalMax(value = "1.0", message = "minConfidence должен быть <= 1.0")
            Double minConfidence,
            HttpServletRequest request) {
        try {
            // Валидация minConfidence (если указан)
            if (minConfidence != null && (minConfidence < 0.0 || minConfidence > 1.0)) {
                LOGGER.warn("⚠️ Некорректное значение minConfidence: {} (должно быть от 0.0 до 1.0)", minConfidence);
                return ResponseEntity.badRequest().body(null);
            }
            
            String language = getLanguageFromHeaderOrUser(request);
            LOGGER.info("🤖 Предложение категорий для стикерсета ID: {}, apply={}, minConfidence={}", 
                id, apply, minConfidence);
            
            // Проверка прав доступа (владелец или админ)
            Long currentUserId = getCurrentUserId();
            StickerSet stickerSet = stickerSetService.findById(id);
            if (stickerSet == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!isOwnerOrAdmin(stickerSet.getUserId(), currentUserId)) {
                LOGGER.warn("⚠️ Пользователь {} попытался категоризовать чужой стикерсет {}", currentUserId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CategorySuggestionResult result = autoCategorizationService.suggestCategoriesForStickerSet(
                id, apply, language, minConfidence);
            
            LOGGER.info("✅ Категории для стикерсета {} предложены (apply={}, применено: {})", 
                id, apply, apply ? result.getSuggestedCategories().size() : "N/A");
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректные данные для предложения категорий стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при предложении категорий стикерсета {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
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
            
            if (authentication != null && authentication.isAuthenticated()) {
                Long currentUserId = Long.valueOf(authentication.getName());
                
                // Проверяем: админ или владелец стикерсета
                boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                boolean isOwner = existingStickerSet.getUserId() != null && existingStickerSet.getUserId().equals(currentUserId);
                
                if (!isAdmin && !isOwner) {
                    LOGGER.warn("⚠️ Пользователь {} попытался изменить видимость чужого стикерсета {}", currentUserId, id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(java.util.Map.of(
                            "error", "Доступ запрещен",
                            "message", "Вы можете изменять видимость только своих стикерсетов"
                        ));
                }
                
                LOGGER.debug("✅ Проверка прав на изменение видимости пройдена: isAdmin={}, isOwner={}", isAdmin, isOwner);
            }
            
            StickerSet updatedStickerSet = stickerSetService.updateVisibility(id, isPublic);
            StickerSetDto updatedDto = StickerSetDto.fromEntity(updatedStickerSet);
            
            LOGGER.info("✅ Стикерсет {} {}", id, action);
            return ResponseEntity.ok(updatedDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при изменении видимости стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(java.util.Map.of(
                    "error", "Ошибка валидации",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при изменении видимости стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "error", "Внутренняя ошибка сервера",
                    "message", "Произошла непредвиденная ошибка при изменении видимости стикерсета"
                ));
        }
    }
    
    /**
     * Заблокировать стикерсет (только для админа)
     */
    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Заблокировать стикерсет",
        description = "Блокирует стикерсет (доступно только админу). " +
                     "Заблокированные стикерсеты не отображаются в галерее и в профилях пользователей."
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
                        "blockReason": "Нарушение правил сообщества",
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
            @Parameter(description = "Причина блокировки", required = false)
            @RequestBody(required = false) java.util.Map<String, String> request) {
        try {
            LOGGER.info("🚫 Блокировка стикерсета с ID: {}", id);
            
            String reason = request != null ? request.get("reason") : null;
            if (reason == null || reason.trim().isEmpty()) {
                reason = "Нарушение правил сообщества";
            }
            
            StickerSet blockedStickerSet = stickerSetService.blockStickerSet(id, reason);
            StickerSetDto blockedDto = StickerSetDto.fromEntity(blockedStickerSet);
            
            LOGGER.info("✅ Стикерсет {} заблокирован по причине: {}", id, reason);
            return ResponseEntity.ok(blockedDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при блокировке стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(java.util.Map.of(
                    "error", "Ошибка валидации",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при блокировке стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "error", "Внутренняя ошибка сервера",
                    "message", "Произошла непредвиденная ошибка при блокировке стикерсета"
                ));
        }
    }
    
    /**
     * Разблокировать стикерсет (только для админа)
     */
    @PutMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
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
            
            StickerSet unblockedStickerSet = stickerSetService.unblockStickerSet(id);
            StickerSetDto unblockedDto = StickerSetDto.fromEntity(unblockedStickerSet);
            
            LOGGER.info("✅ Стикерсет {} разблокирован", id);
            return ResponseEntity.ok(unblockedDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка при разблокировке стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(java.util.Map.of(
                    "error", "Ошибка валидации",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при разблокировке стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "error", "Внутренняя ошибка сервера",
                    "message", "Произошла непредвиденная ошибка при разблокировке стикерсета"
                ));
        }
    }
    
    /**
     * Отметить стикерсет как официальный (только для админа)
     */
    @PutMapping("/{id}/official")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Отметить как официальный",
        description = "Устанавливает флаг isOfficial=true для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет отмечен как официальный",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может изменять официальный статус"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> markStickerSetOfficial(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("🏅 Установка официального статуса для стикерсета {}", id);
            StickerSet updated = stickerSetService.setOfficial(id);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(java.util.Map.of(
                    "error", "Не найдено",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при установке официального статуса стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Снять признак официального стикерсета (только для админа)
     */
    @PutMapping("/{id}/unofficial")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Снять официальный статус",
        description = "Устанавливает флаг isOfficial=false для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет отмечен как неофициальный",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может изменять официальный статус"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> markStickerSetUnofficial(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("🏷️ Снятие официального статуса для стикерсета {}", id);
            StickerSet updated = stickerSetService.unsetOfficial(id);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(java.util.Map.of(
                    "error", "Не найдено",
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при снятии официального статуса стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Установить автора стикерсета (только для админа)
     */
    @PutMapping("/{id}/author")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Установить автора стикерсета",
        description = "Устанавливает Telegram ID автора (authorId) для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Автор установлен",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = "{\"authorId\":123456789}"))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может устанавливать автора"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> setStickerSetAuthor(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Тело запроса с authorId", required = true)
            @RequestBody java.util.Map<String, Long> request) {
        try {
            if (request == null || !request.containsKey("authorId")) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "Ошибка валидации",
                    "message", "Поле authorId обязательно"
                ));
            }
            Long authorId = request.get("authorId");
            StickerSet updated = stickerSetService.setAuthor(id, authorId);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "error", "Ошибка валидации",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при установке автора для стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Очистить автора стикерсета (только для админа)
     */
    @DeleteMapping("/{id}/author")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Очистить автора стикерсета",
        description = "Очищает Telegram ID автора (authorId=null) для стикерсета (доступно только админу)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Автор очищен",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - только админ может очищать автора"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> clearStickerSetAuthor(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            StickerSet updated = stickerSetService.clearAuthor(id);
            return ResponseEntity.ok(StickerSetDto.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of(
                "error", "Не найдено",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при очистке автора стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить топ стикерсетов по лайкам
     */
    @GetMapping("/top-bylikes")
    @Operation(
        summary = "Получить топ стикерсетов по лайкам",
        description = "Возвращает список стикерсетов, отсортированных по количеству лайков (по убыванию). " +
                     "Поддерживает локализацию названий категорий через заголовок X-Language (ru/en) или автоматически из initData пользователя. " +
                     "Включает информацию о том, лайкнул ли текущий пользователь каждый стикерсет."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Топ стикерсетов по лайкам успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class),
                examples = {
                    @ExampleObject(name = "Обычный топ", value = """
                    {
                        "content": [
                            {
                                "id": 5,
                                "userId": 123456789,
                                "title": "Популярные стикеры",
                                "name": "popular_stickers_by_StickerGalleryBot",
                                "createdAt": "2025-01-15T10:30:00",
                                "likesCount": 42,
                                "isLikedByCurrentUser": true,
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
                    """),
                    @ExampleObject(name = "Топ официальных (officialOnly=true)", value = """
                    {
                        "content": [
                            {
                                "id": 6,
                                "userId": 333333333,
                                "title": "Оф топ",
                                "name": "official_top_by_StickerGalleryBot",
                                "isOfficial": true,
                                "likesCount": 99,
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
                    """),
                    @ExampleObject(name = "Топ по автору (authorId=123456789)", value = """
                    {
                        "content": [
                            {
                                "id": 7,
                                "userId": 444444444,
                                "title": "Авторский топ",
                                "name": "author_top_by_StickerGalleryBot",
                                "authorId": 123456789,
                                "likesCount": 50,
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
                    """),
                    @ExampleObject(name = "Топ только авторских (hasAuthorOnly=true)", value = """
                    {
                        "content": [
                            {
                                "id": 8,
                                "userId": 555555555,
                                "title": "Авторские",
                                "name": "authored_top_by_StickerGalleryBot",
                                "authorId": 999999999,
                                "likesCount": 25,
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
                    """)
                })),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getTopStickerSetsByLikes(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Показывать только официальные стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean officialOnly,
            @Parameter(description = "Фильтр по автору (Telegram ID)", example = "123456789")
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Показывать только авторские стикерсеты (authorId IS NOT NULL)", example = "false")
            @RequestParam(defaultValue = "false") boolean hasAuthorOnly,
            HttpServletRequest request) {
        try {
            LOGGER.info("🏆 Получение топ стикерсетов по лайкам с пагинацией: page={}, size={}, officialOnly={}, authorId={}, hasAuthorOnly={}", page, size, officialOnly, authorId, hasAuthorOnly);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("likesCount");
            pageRequest.setDirection("DESC");
            
            String language = getLanguageFromHeaderOrUser(request);
            Long currentUserId = getCurrentUserIdOrNull();
            PageResponse<StickerSetWithLikesDto> result = likeService.getTopStickerSetsByLikes(pageRequest, language, currentUserId, officialOnly, authorId, hasAuthorOnly);
            
            // Конвертируем StickerSetWithLikesDto в StickerSetDto для совместимости
            // Создаем временную Page для использования с PageResponse.of
            Page<StickerSetWithLikesDto> tempPage = new PageImpl<>(
                result.getContent(),
                org.springframework.data.domain.PageRequest.of(result.getPage(), result.getSize()),
                result.getTotalElements()
            );
            
            PageResponse<StickerSetDto> convertedResult = PageResponse.of(
                tempPage,
                result.getContent().stream()
                    .map(StickerSetWithLikesDto::getStickerSet)
                    .collect(Collectors.toList())
            );
            
            LOGGER.debug("✅ Найдено {} топ стикерсетов на странице {} из {}", 
                    convertedResult.getContent().size(), convertedResult.getPage() + 1, convertedResult.getTotalPages());
            return ResponseEntity.ok(convertedResult);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении топа стикерсетов по лайкам: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Извлечь ID текущего пользователя из SecurityContext (может вернуть null)
     */
    private Long getCurrentUserIdOrNull() {
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
    
    /**
     * Извлечь ID текущего пользователя (с исключением если не авторизован)
     */
    private Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        return userId;
    }

    private String languageResponse(String language, String ruMessage, String enMessage) {
        return "ru".equals(language) ? ruMessage : enMessage;
    }

    private String languageResponse(HttpServletRequest request, String ruMessage, String enMessage) {
        return languageResponse(getLanguageFromHeaderOrUser(request), ruMessage, enMessage);
    }
    
    /**
     * Проверка, является ли пользователь владельцем или админом
     */
    private boolean isOwnerOrAdmin(Long ownerId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        
        // Если текущий пользователь является владельцем
        if (currentUserId.equals(ownerId)) {
            return true;
        }
        
        // Проверяем, является ли пользователь администратором
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при проверке прав администратора: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Проверка, является ли пользователь админом
     */
    private boolean isAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при проверке прав администратора: {}", e.getMessage());
        }
        return false;
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
} 