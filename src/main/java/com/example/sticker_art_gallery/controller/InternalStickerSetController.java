package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
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

    public InternalStickerSetController(StickerSetService stickerSetService) {
        this.stickerSetService = stickerSetService;
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

        if (createDto.getIsPublic() == null) {
            createDto.setIsPublic(true);
        }

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
            HttpServletRequest request) {
        try {
            String language = resolveLanguage(request);
            LOGGER.info("🔍 [internal] Поиск авторских стикерсетов: authorId={}, page={}, size={}, sort={}, direction={}, categoryKeys={}, shortInfo={}, language={}",
                    authorId, page, size, sort, direction, categoryKeys, shortInfo, language);

            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);

            Set<String> categoryKeySet = parseCategoryKeys(categoryKeys);
            boolean includePrivate = true; // доверенный межсервисный вызов

            PageResponse<StickerSetDto> result = stickerSetService.findByAuthorIdWithPagination(
                    authorId,
                    pageRequest,
                    categoryKeySet,
                    null,
                    includePrivate,
                    shortInfo,
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

