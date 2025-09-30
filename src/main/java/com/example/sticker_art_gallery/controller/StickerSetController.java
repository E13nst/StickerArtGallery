package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*") // Разрешаем CORS для фронтенда
@Tag(name = "Стикерсеты", description = "Управление стикерсетами пользователей")
@SecurityRequirement(name = "TelegramInitData")
public class StickerSetController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetController.class);
    private final StickerSetService stickerSetService;
    
    @Autowired
    public StickerSetController(StickerSetService stickerSetService) {
        this.stickerSetService = stickerSetService;
    }
    
    /**
     * Получить все стикерсеты с пагинацией
     */
    @GetMapping
    @Operation(
        summary = "Получить все стикерсеты с пагинацией",
        description = "Возвращает список всех стикерсетов в системе с пагинацией и обогащением данных из Telegram Bot API. Требует авторизации через Telegram Web App."
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
                                "telegramStickerSetInfo": "{\\"name\\":\\"my_stickers_by_StickerGalleryBot\\",\\"title\\":\\"Мои стикеры\\",\\"sticker_type\\":\\"regular\\",\\"is_animated\\":false,\\"stickers\\":[...]}"
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
            @RequestParam(defaultValue = "DESC") @Pattern(regexp = "ASC|DESC") String direction) {
        try {
            LOGGER.info("📋 Получение всех стикерсетов с пагинацией: page={}, size={}, sort={}, direction={}", 
                    page, size, sort, direction);
            
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            PageResponse<StickerSetDto> result = stickerSetService.findAllWithPagination(pageRequest);
            
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
        description = "Создает новый стикерсет для пользователя. Все поля обязательны для заполнения."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Стикерсет успешно создан",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 5,
                        "userId": 123456789,
                        "title": "Новые стикеры",
                        "name": "new_stickers_by_StickerGalleryBot",
                        "createdAt": "2025-09-15T14:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные - ошибки валидации",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "validationErrors": {
                        "userId": "ID пользователя должен быть положительным числом",
                        "title": "Название стикерсета не может быть пустым",
                        "name": "Имя стикерсета может содержать только латинские буквы, цифры и подчеркивания"
                    },
                    "error": "Ошибка валидации",
                    "message": "Некорректные данные в запросе"
                }
                """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> createStickerSet(
            @Parameter(description = "Данные для создания стикерсета", required = true)
            @Valid @RequestBody StickerSetDto stickerSetDto) {
        try {
            LOGGER.info("➕ Создание нового стикерсета: {}", stickerSetDto.getTitle());
            
            StickerSet newStickerSet = stickerSetService.createStickerSet(
                stickerSetDto.getUserId(),
                stickerSetDto.getTitle(),
                stickerSetDto.getName()
            );
            
            StickerSetDto createdDto = StickerSetDto.fromEntity(newStickerSet);
            LOGGER.info("✅ Стикерсет создан с ID: {}", createdDto.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании стикерсета", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить существующий стикерсет
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить стикерсет",
        description = "Обновляет существующий стикерсет. Можно изменить title и name. ID и userId не изменяются."
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
} 