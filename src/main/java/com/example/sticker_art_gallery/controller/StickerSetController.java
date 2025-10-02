package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetWithLikesDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
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
    
    @Autowired
    public StickerSetController(StickerSetService stickerSetService, LikeService likeService) {
        this.stickerSetService = stickerSetService;
        this.likeService = likeService;
    }
    
    /**
     * Получить все стикерсеты с пагинацией
     */
    @GetMapping
    @Operation(
        summary = "Получить все стикерсеты с пагинацией и фильтрацией",
        description = "Возвращает список всех стикерсетов в системе с пагинацией, фильтрацией по категориям и обогащением данных из Telegram Bot API. " +
                     "Поддерживает локализацию названий категорий. " +
                     "Можно фильтровать по категориям через параметр categoryKeys. " +
                     "Требует авторизации через Telegram Web App."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список стикерсетов успешно получен",
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
                                    },
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
                        "totalElements": 156,
                        "totalPages": 8,
                        "first": true,
                        "last": false,
                        "hasNext": true,
                        "hasPrevious": false
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или проблемы с Telegram Bot API")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getAllStickerSets(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Поле для сортировки", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Направление сортировки", example = "DESC")
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction,
            @Parameter(description = "Код языка для локализации категорий (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language,
            @Parameter(description = "Фильтр по ключам категорий (через запятую)", example = "animals,memes")
            @RequestParam(required = false) String categoryKeys) {
        try {
            LOGGER.info("📋 Получение всех стикерсетов с пагинацией: page={}, size={}, sort={}, direction={}, categoryKeys={}", 
                    page, size, sort, direction, categoryKeys);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            PageResponse<StickerSetDto> result;
            Long currentUserId = getCurrentUserIdOrNull();
            if (categoryKeys != null && !categoryKeys.trim().isEmpty()) {
                // Фильтрация по категориям
                String[] categoryKeyArray = categoryKeys.split(",");
                result = stickerSetService.findByCategoryKeys(categoryKeyArray, pageRequest, language, currentUserId);
            } else {
                // Без фильтрации
                result = stickerSetService.findAllWithPagination(pageRequest, language, currentUserId);
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
        description = "Возвращает информацию о стикерсете по его уникальному идентификатору."
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
            StickerSetDto dto = stickerSetService.findByIdWithBotApiData(id);
            
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
        description = "Возвращает все стикерсеты, созданные конкретным пользователем, с пагинацией и обогащением данных из Telegram Bot API."
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
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction) {
        try {
            LOGGER.info("🔍 Поиск стикерсетов для пользователя: {} с пагинацией: page={}, size={}, sort={}, direction={}", 
                    userId, page, size, sort, direction);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            PageResponse<StickerSetDto> result = stickerSetService.findByUserIdWithPagination(userId, pageRequest);
            
            LOGGER.debug("✅ Найдено {} стикерсетов для пользователя {} на странице {} из {}", 
                    result.getContent().size(), userId, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске стикерсетов для пользователя: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            Создает новый стикерсет для пользователя с расширенной валидацией и автоматическим заполнением данных.
            
            **Обязательные поля:**
            - `name` - уникальное имя стикерсета для Telegram API или URL стикерсета
            
            **Опциональные поля:**
            - `userId` - ID пользователя (если не указан, извлекается из initData)
            - `title` - название стикерсета (если не указано, получается из Telegram API)
            - `categoryKeys` - массив ключей категорий для стикерсета (например, ["animals", "cute"])
            
            **Процесс валидации:**
            1. Проверка уникальности имени в базе данных
            2. Валидация существования стикерсета в Telegram API
            3. Автоматическое заполнение недостающих данных
            4. Создание записи в базе данных
            
            **Требования:**
            - Авторизация через Telegram Web App (initData)
            - Стикерсет должен существовать в Telegram
            - Имя стикерсета должно быть уникальным в галерее
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
                @ExampleObject(name = "Отсутствует userId", value = """
                    {
                        "error": "Ошибка валидации",
                        "message": "Не удалось определить ID пользователя. Укажите userId или убедитесь, что вы авторизованы через Telegram Web App"
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
    public ResponseEntity<?> createStickerSet(
            @Parameter(description = """
                Данные для создания стикерсета.
                
                **Обязательные поля:**
                - `name` - имя стикерсета или URL стикерсета (строка, 1-200 символов)
                
                **Опциональные поля:**
                - `userId` - ID пользователя (положительное число, если не указан - извлекается из initData)
                - `title` - название стикерсета (строка до 64 символов, если не указано - получается из Telegram API)
                - `categoryKeys` - массив ключей категорий (например, ["animals", "cute"])
                
                **Поддерживаемые форматы для поля name:**
                - Имя стикерсета: `my_stickers_by_StickerGalleryBot`
                - URL стикерсета: `https://t.me/addstickers/ShaitanChick`
                
                **Примеры запросов:**
                - Минимальный (имя): `{"name": "my_stickers_by_StickerGalleryBot"}`
                - Минимальный (URL): `{"name": "https://t.me/addstickers/ShaitanChick"}`
                - С title: `{"name": "my_stickers", "title": "Мои стикеры"}`
                - Полный: `{"name": "my_stickers", "title": "Мои стикеры", "userId": 123456789}`
                """, required = true)
            @Valid @RequestBody CreateStickerSetDto createDto) {
        try {
            LOGGER.info("➕ Создание нового стикерсета: {}", createDto.getName());
            
            StickerSet newStickerSet = stickerSetService.createStickerSet(createDto);
            StickerSetDto createdDto = StickerSetDto.fromEntity(newStickerSet);
            
            LOGGER.info("✅ Стикерсет создан с ID: {} (title: '{}', userId: {})", 
                       createdDto.getId(), createdDto.getTitle(), createdDto.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации при создании стикерсета: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(java.util.Map.of(
                    "error", "Ошибка валидации",
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
     * Обновить существующий стикерсет
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить стикерсет",
        description = "Обновляет существующий стикерсет. Администратор может обновлять любые стикерсеты, обычный пользователь - только свои. Можно изменить title и name. ID и userId не изменяются."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет успешно обновлен",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "userId": 123456789,
                        "title": "Обновленные стикеры",
                        "name": "updated_stickers_by_StickerGalleryBot",
                        "createdAt": "2025-09-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные - ошибки валидации"),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - можно обновлять только свои стикерсеты"),
        @ApiResponse(responseCode = "404", description = "Стикерсет с указанным ID не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> updateStickerSet(
            @Parameter(description = "ID стикерсета для обновления", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Новые данные стикерсета", required = true)
            @Valid @RequestBody StickerSetDto stickerSetDto) {
        try {
            LOGGER.info("✏️ Обновление стикерсета с ID: {}", id);
            
            StickerSet existingStickerSet = stickerSetService.findById(id);
            if (existingStickerSet == null) {
                LOGGER.warn("⚠️ Стикерсет с ID {} не найден для обновления", id);
                return ResponseEntity.notFound().build();
            }
            
            // Проверяем права доступа
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() instanceof com.example.sticker_art_gallery.model.user.UserEntity) {
                com.example.sticker_art_gallery.model.user.UserEntity currentUser = 
                    (com.example.sticker_art_gallery.model.user.UserEntity) authentication.getPrincipal();
                
                // Проверяем: админ или владелец стикерсета
                boolean isAdmin = currentUser.getRole() == com.example.sticker_art_gallery.model.user.UserEntity.UserRole.ADMIN;
                boolean isOwner = existingStickerSet.getUserId().equals(currentUser.getId());
                
                if (!isAdmin && !isOwner) {
                    LOGGER.warn("⚠️ Пользователь {} попытался обновить чужой стикерсет {}", currentUser.getId(), id);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
                
                LOGGER.debug("✅ Проверка прав на обновление пройдена: isAdmin={}, isOwner={}", isAdmin, isOwner);
            }
            
            // Обновляем поля
            if (stickerSetDto.getTitle() != null) {
                existingStickerSet.setTitle(stickerSetDto.getTitle());
            }
            if (stickerSetDto.getName() != null) {
                existingStickerSet.setName(stickerSetDto.getName());
            }
            
            StickerSet updatedStickerSet = stickerSetService.save(existingStickerSet);
            StickerSetDto updatedDto = StickerSetDto.fromEntity(updatedStickerSet);
            
            LOGGER.info("✅ Стикерсет обновлен: {}", updatedDto.getTitle());
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении стикерсета с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            
            if (authentication != null && authentication.getPrincipal() instanceof com.example.sticker_art_gallery.model.user.UserEntity) {
                com.example.sticker_art_gallery.model.user.UserEntity currentUser = 
                    (com.example.sticker_art_gallery.model.user.UserEntity) authentication.getPrincipal();
                
                // Проверяем: админ или владелец стикерсета
                boolean isAdmin = currentUser.getRole() == com.example.sticker_art_gallery.model.user.UserEntity.UserRole.ADMIN;
                boolean isOwner = existingStickerSet.getUserId().equals(currentUser.getId());
                
                if (!isAdmin && !isOwner) {
                    LOGGER.warn("⚠️ Пользователь {} попытался удалить чужой стикерсет {}", currentUser.getId(), id);
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
            @Parameter(description = "Код языка для ответа (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language) {
        try {
            LOGGER.info("🏷️ Обновление категорий стикерсета с ID: {}, категории: {}", id, categoryKeys);
            
            StickerSet updatedStickerSet = stickerSetService.updateCategories(id, categoryKeys);
            
            LOGGER.info("✅ Категории стикерсета {} успешно обновлены", id);
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
     * Получить топ стикерсетов по лайкам
     */
    @GetMapping("/top-bylikes")
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
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getTopStickerSetsByLikes(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Количество элементов на странице (1-100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Код языка для локализации категорий (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language) {
        try {
            LOGGER.info("🏆 Получение топ стикерсетов по лайкам с пагинацией: page={}, size={}", page, size);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort("likesCount");
            pageRequest.setDirection("DESC");
            
            Long currentUserId = getCurrentUserIdOrNull();
            PageResponse<StickerSetWithLikesDto> result = likeService.getTopStickerSetsByLikes(pageRequest, language, currentUserId);
            
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
} 