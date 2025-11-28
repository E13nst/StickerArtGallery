package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Межсервисный контроллер для создания стикерсетов от имени пользователей.
 */
@RestController
@RequestMapping("/internal/stickersets")
@Tag(name = "Internal Sticker Sets", description = "Эндпоинты для межсервисного создания стикерсетов в галерее")
@SecurityRequirement(name = "ServiceToken")
@Validated
public class InternalStickerSetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalStickerSetController.class);

    private final StickerSetService stickerSetService;
    private final StickerSetRepository stickerSetRepository;

    public InternalStickerSetController(StickerSetService stickerSetService, StickerSetRepository stickerSetRepository) {
        this.stickerSetService = stickerSetService;
        this.stickerSetRepository = stickerSetRepository;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Получить стикерсет по ID (межсервисный вызов)",
        description = """
            Межсервисный эндпоинт, повторяющий логику публичного GET /api/stickersets/{id}, но с авторизацией по сервисному токену.
            Возвращает информацию о стикерсете, включая связанные данные, с возможностью отключить Telegram Bot API через параметр shortInfo.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Стикерсет найден",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "400", description = "Некорректный ID"),
        @ApiResponse(responseCode = "401", description = "Межсервисная авторизация не пройдена"),
        @ApiResponse(responseCode = "403", description = "Нет прав для выполнения операции"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetDto> getStickerSetByIdInternal(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            HttpServletRequest request) {
        try {
            String language = resolveLanguage(request);
            LOGGER.info("🔍 [internal] Получение стикерсета по ID {} (shortInfo={}, language={})", id, shortInfo, language);

            StickerSetDto dto = stickerSetService.findByIdWithBotApiData(id, language, null, shortInfo);
            if (dto == null) {
                LOGGER.warn("⚠️ [internal] Стикерсет с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректный ID для внутреннего запроса стикерсета: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при внутреннем получении стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Создать стикерсет от имени пользователя",
        description = """
            Межсервисный эндпоинт для регистрации стикерсета Telegram в галерее.
            Токен сервиса должен быть передан в заголовке `X-Service-Token`.
            """,
        parameters = {
            @Parameter(
                name = "userId",
                in = ParameterIn.QUERY,
                required = true,
                description = "Telegram ID пользователя, от имени которого создаётся стикерсет",
                example = "123456789"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Стикерсет успешно создан",
            content = @Content(schema = @Schema(implementation = StickerSetDto.class))),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
        @ApiResponse(responseCode = "401", description = "Межсервисная авторизация не пройдена"),
        @ApiResponse(responseCode = "403", description = "Нет прав для выполнения операции"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        content = @Content(
            schema = @Schema(implementation = CreateStickerSetDto.class),
            examples = @ExampleObject(value = """
                {
                  "name": "https://t.me/addstickers/my_pack_by_bot",
                  "title": "Мои стикеры",
                  "categoryKeys": ["animals", "cute"],
                  "isPublic": true
                }
                """)
        )
    )
    public ResponseEntity<?> createStickerSetForUser(
            @Valid @RequestBody CreateStickerSetDto createDto,
            @RequestParam @NotNull @Positive Long userId,
            @Parameter(
                name = "authorId",
                in = ParameterIn.QUERY,
                description = "Telegram ID автора стикерсета (опционально). Если задан, будет сохранён в authorId.",
                example = "123456789"
            )
            @RequestParam(required = false) @Positive Long authorId,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            HttpServletRequest request) {

        // Visibility устанавливается в сервисе по умолчанию (PRIVATE для internal API)

        try {
            String language = resolveLanguage(request);
            LOGGER.info("🤝 Межсервисное создание стикерсета для userId {}: {} (language={}, shortInfo={}, authorId={})",
                    userId, createDto.getName(), language, shortInfo, authorId);
            StickerSet stickerSet = stickerSetService.createStickerSetForUser(createDto, userId, language, authorId);
            StickerSetDto responseDto = stickerSetService.findByIdWithBotApiData(stickerSet.getId(), language, userId, shortInfo);
            if (responseDto == null) {
                responseDto = StickerSetDto.fromEntity(stickerSet, language, userId);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("⚠️ Ошибка валидации при межсервисном создании стикерсета: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Validation error",
                            "message", ex.getMessage()
                    ));
        } catch (Exception ex) {
            LOGGER.error("❌ Внутренняя ошибка при межсервисном создании стикерсета", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal error",
                            "message", "Unexpected error while creating stickerset"
                    ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Удалить стикерсет (межсервисный вызов)",
        description = "Полная версия публичного DELETE /api/stickersets/{id} с авторизацией по сервисному токену."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Стикерсет успешно удален"),
        @ApiResponse(responseCode = "400", description = "Некорректный ID"),
        @ApiResponse(responseCode = "401", description = "Межсервисная авторизация не пройдена"),
        @ApiResponse(responseCode = "403", description = "Нет прав для выполнения операции"),
        @ApiResponse(responseCode = "404", description = "Стикерсет не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Void> deleteStickerSetInternal(
            @Parameter(description = "ID стикерсета для удаления", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("🗑️ [internal] Удаление стикерсета {}", id);
            StickerSet existingStickerSet = stickerSetService.findById(id);
            if (existingStickerSet == null) {
                return ResponseEntity.notFound().build();
            }

            stickerSetService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректный ID для удаления: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при внутреннем удалении стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Опубликовать стикерсет (межсервисный вызов)",
        description = "Опубликовать стикерсет (PRIVATE -> PUBLIC) с начислением ART за первую публикацию."
    )
    public ResponseEntity<?> publishStickerSetInternal(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("👁️ [internal] Публикация стикерсета {}", id);
            StickerSet stickerSet = stickerSetService.publishStickerSet(id);
            StickerSetDto dto = StickerSetDto.fromEntity(stickerSet);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ [internal] Ошибка при публикации стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("❌ [internal] Ошибка при публикации стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Внутренняя ошибка сервера"));
        }
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Сделать стикерсет приватным (межсервисный вызов)",
        description = "Сделать стикерсет приватным (PUBLIC -> PRIVATE)."
    )
    public ResponseEntity<?> unpublishStickerSetInternal(
            @Parameter(description = "ID стикерсета", required = true, example = "1")
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        try {
            LOGGER.info("👁️ [internal] Скрытие стикерсета {}", id);
            StickerSet stickerSet = stickerSetService.unpublishStickerSet(id);
            StickerSetDto dto = StickerSetDto.fromEntity(stickerSet);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ [internal] Ошибка при скрытии стикерсета {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("❌ [internal] Ошибка при скрытии стикерсета {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Внутренняя ошибка сервера"));
        }
    }

    @GetMapping("/check")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Проверить наличие стикерсета в галерее (межсервисный вызов)",
        description = """
            Межсервисный эндпоинт для проверки наличия стикерсета в галерее по имени или URL.
            Принимает либо параметр name (имя стикерсета), либо url (URL вида https://t.me/addstickers/taxiderm).
            Если передан URL, извлекает имя стикерсета из него.
            Возвращает информацию о наличии стикерсета в базе данных.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Проверка выполнена успешно",
            content = @Content(schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                        "exists": true,
                        "name": "taxiderm"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры (не указаны name или url)"),
        @ApiResponse(responseCode = "401", description = "Межсервисная авторизация не пройдена"),
        @ApiResponse(responseCode = "403", description = "Нет прав для выполнения операции"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Map<String, Object>> checkStickerSetExists(
            @Parameter(description = "Имя стикерсета", example = "taxiderm")
            @RequestParam(required = false) String name,
            @Parameter(description = "URL стикерсета", example = "https://t.me/addstickers/taxiderm")
            @RequestParam(required = false) String url) {
        try {
            // Валидация: хотя бы один параметр должен быть передан
            if ((name == null || name.trim().isEmpty()) && (url == null || url.trim().isEmpty())) {
                LOGGER.warn("⚠️ [internal] Запрос проверки стикерсета без параметров name или url");
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Validation error",
                                "message", "Either 'name' or 'url' parameter must be provided"
                        ));
            }

            String stickerSetName;
            
            // Если передан URL, извлекаем имя из него
            if (url != null && !url.trim().isEmpty()) {
                try {
                    CreateStickerSetDto dto = new CreateStickerSetDto();
                    stickerSetName = dto.extractStickerSetNameFromUrl(url);
                    LOGGER.debug("🔍 [internal] Извлечено имя '{}' из URL '{}'", stickerSetName, url);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("⚠️ [internal] Некорректный URL стикерсета: {}", e.getMessage());
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "error", "Validation error",
                                    "message", "Invalid sticker set URL: " + e.getMessage()
                            ));
                }
            } else {
                // Используем переданное имя
                if (name == null || name.trim().isEmpty()) {
                    LOGGER.warn("⚠️ [internal] Пустое имя стикерсета");
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "error", "Validation error",
                                    "message", "Sticker set name cannot be empty"
                            ));
                }
                stickerSetName = name.trim();
            }

            // Нормализуем имя (приводим к нижнему регистру)
            stickerSetName = stickerSetName.toLowerCase().trim();
            
            if (stickerSetName.isEmpty()) {
                LOGGER.warn("⚠️ [internal] Пустое имя стикерсета после нормализации");
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Validation error",
                                "message", "Sticker set name cannot be empty"
                        ));
            }

            LOGGER.info("🔍 [internal] Проверка наличия стикерсета '{}' в галерее", stickerSetName);

            // Проверяем наличие в базе данных
            boolean exists = stickerSetRepository.findByNameIgnoreCase(stickerSetName).isPresent();

            Map<String, Object> response = Map.of(
                    "exists", exists,
                    "name", stickerSetName
            );

            LOGGER.debug("✅ [internal] Результат проверки стикерсета '{}': exists={}", stickerSetName, exists);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOGGER.error("❌ [internal] Ошибка при проверке наличия стикерсета", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal error",
                            "message", "Unexpected error while checking stickerset existence"
                    ));
        }
    }

    @GetMapping("/author/{authorId}")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(
        summary = "Получить авторские стикерсеты (межсервисный вызов)",
        description = """
            Межсервисный эндпоинт для получения авторских стикерсетов.
            Использует сервисный токен, аналогично POST /internal/stickersets.
            Возвращает все стикерсеты автора (включая приватные), без фильтрации прав текущего пользователя.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список авторских стикерсетов получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры"),
        @ApiResponse(responseCode = "401", description = "Межсервисная авторизация не пройдена"),
        @ApiResponse(responseCode = "403", description = "Нет прав для выполнения операции"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getStickerSetsByAuthorIdInternal(
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
            @RequestParam(required = false) String categoryKeys,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            @Parameter(description = "Режим превью: возвращать только 3 случайных стикера в telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean preview,
            HttpServletRequest request) {
        try {
            String language = resolveLanguage(request);
            LOGGER.info("🔍 [internal] Поиск авторских стикерсетов: authorId={}, page={}, size={}, sort={}, direction={}, categoryKeys={}, shortInfo={}, preview={}, language={}",
                    authorId, page, size, sort, direction, categoryKeys, shortInfo, preview, language);

            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);

            Set<String> categoryKeySet = parseCategoryKeys(categoryKeys);
            // Доверенный межсервисный вызов - показываем все стикерсеты (публичные + приватные)
            com.example.sticker_art_gallery.dto.VisibilityFilter visibilityFilter = 
                com.example.sticker_art_gallery.dto.VisibilityFilter.ALL;

            PageResponse<StickerSetDto> result = stickerSetService.findByAuthorIdWithPagination(
                    authorId,
                    pageRequest,
                    categoryKeySet,
                    null,  // currentUserId - для межсервисных вызовов не требуется
                    visibilityFilter,
                    null,  // type - не фильтруем
                    shortInfo,
                    preview,
                    normalizeLanguage(language)
            );

            LOGGER.debug("✅ [internal] Найдено {} авторских стикерсетов для authorId {} на странице {} из {}",
                    result.getContent().size(), authorId, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректные параметры для внутреннего запроса авторских стикерсетов {}: {}", authorId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("❌ Внутренняя ошибка при получении авторских стикерсетов {}: {}", authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Set<String> parseCategoryKeys(String categoryKeys) {
        if (categoryKeys == null || categoryKeys.trim().isEmpty()) {
            return null;
        }
        Set<String> result = java.util.Arrays.stream(categoryKeys.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return result.isEmpty() ? null : result;
    }

    private String resolveLanguage(HttpServletRequest request) {
        if (request != null) {
            String header = request.getHeader("X-Language");
            if (header != null && !header.isBlank()) {
                return normalizeLanguage(header);
            }
        }
        return "en";
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase();
        return ("ru".equals(normalized)) ? "ru" : "en";
    }
}

