package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.StickerSetStatisticsDto;
import com.example.sticker_art_gallery.dto.VisibilityFilter;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.service.statistics.StatisticsService;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Set;

/**
 * Контроллер для специализированных запросов стикерсетов
 */
@RestController
@RequestMapping("/api/stickersets")
@CrossOrigin(origins = "*")
@Tag(name = "Специализированные запросы стикерсетов", description = "Запросы стикерсетов по пользователю, автору и статистика")
@SecurityRequirement(name = "TelegramInitData")
public class StickerSetQueryController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetQueryController.class);
    private final StickerSetService stickerSetService;
    private final StatisticsService statisticsService;
    private final StickerSetControllerHelper helper;
    
    @Autowired
    public StickerSetQueryController(StickerSetService stickerSetService,
                                    StatisticsService statisticsService,
                                    StickerSetControllerHelper helper) {
        this.stickerSetService = stickerSetService;
        this.statisticsService = statisticsService;
        this.helper = helper;
    }
    
    /**
     * Получить стикерсеты конкретного пользователя с фильтрацией
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Получить стикерсеты пользователя",
        description = "Возвращает список стикерсетов конкретного пользователя с пагинацией и фильтрацией. " +
                     "Требует авторизации. " +
                     "По умолчанию показывает все стикерсеты пользователя (публичные и приватные), если текущий пользователь " +
                     "является владельцем или администратором. Для других пользователей показываются только публичные стикерсеты. " +
                     "Параметр visibility позволяет дополнительно фильтровать по видимости: " +
                     "ALL (все), PUBLIC (только публичные), PRIVATE (только приватные). " +
                     "Приватные стикерсеты доступны только владельцу и администратору."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список стикерсетов успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getStickerSetsByUser(
            @Parameter(description = "ID пользователя (Telegram User ID)", required = true, example = "123456789")
            @PathVariable @Positive Long userId,
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
            @Parameter(description = "Фильтр по типу стикерсета (USER, OFFICIAL)", example = "USER")
            @RequestParam(required = false) StickerSetType type,
            @Parameter(description = "Показывать только верифицированные стикерсеты (isVerified=true)", example = "false")
            @RequestParam(required = false) Boolean isVerified,
            @Parameter(description = "Показать только лайкнутые пользователем стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean likedOnly,
            @Parameter(description = "Фильтр видимости: ALL (все), PUBLIC (только публичные), PRIVATE (только приватные)", example = "ALL")
            @RequestParam(defaultValue = "ALL") VisibilityFilter visibility,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            @Parameter(description = "Режим превью: возвращать только 1 случайный стикер в telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean preview,
            HttpServletRequest request) {
        try {
            // Проверка авторизации
            Long currentUserId = helper.getCurrentUserIdOrNull();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Попытка доступа к стикерсетам пользователя без авторизации");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Определяем итоговый фильтр видимости
            VisibilityFilter effectiveVisibility = visibility;
            
            // Определяем, является ли текущий пользователь владельцем или админом
            boolean isOwnerOrAdmin = helper.isOwnerOrAdmin(userId, currentUserId);
            boolean includeBlocked = isOwnerOrAdmin; // Заблокированные видны только владельцу и админу
            
            // Если пользователь не владелец и не админ, принудительно ограничиваем видимость
            if (!isOwnerOrAdmin) {
                // Для чужих стикерсетов можем показывать только публичные
                if (visibility == VisibilityFilter.ALL || 
                    visibility == VisibilityFilter.PRIVATE) {
                    effectiveVisibility = VisibilityFilter.PUBLIC;
                    LOGGER.debug("🔒 Пользователь {} не владелец/админ для userId {}, фильтр изменен на PUBLIC", 
                        currentUserId, userId);
                }
            }
            
            LOGGER.debug("👤 Получение стикерсетов пользователя {}: visibility={}, effectiveVisibility={}, includeBlocked={}", 
                userId, visibility, effectiveVisibility, includeBlocked);
            
            // Построение параметров запроса
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            Set<String> categoryKeysSet = null;
            if (categoryKeys != null && !categoryKeys.trim().isEmpty()) {
                categoryKeysSet = Set.of(categoryKeys.split(","));
            }
            
            String language = helper.getLanguageFromHeaderOrUser(request);
            
            // Вызов сервиса с учетом прав доступа к заблокированным стикерсетам
            PageResponse<StickerSetDto> result = stickerSetService.findByUserIdWithPagination(
                userId,
                pageRequest,
                categoryKeysSet,
                isVerified,
                likedOnly,
                currentUserId,
                effectiveVisibility,
                type,
                shortInfo,
                preview,
                language,
                includeBlocked
            );
            
            LOGGER.debug("✅ Найдено {} стикерсетов пользователя {} на странице {} из {}", 
                result.getContent().size(), userId, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении стикерсетов пользователя {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить стикерсеты конкретного автора с фильтрацией (deprecated: authorId => userId + isVerified)
     */
    @GetMapping("/author/{authorId}")
    @Deprecated
    @Operation(
        summary = "Получить стикерсеты автора (deprecated)",
        deprecated = true,
        description = "Возвращает список стикерсетов конкретного автора с пагинацией и фильтрацией. " +
                     "Требует авторизации. " +
                     "По умолчанию показывает все стикерсеты автора (публичные и приватные), если текущий пользователь " +
                     "является автором или администратором. Для других пользователей показываются только публичные стикерсеты. " +
                     "Параметр visibility позволяет дополнительно фильтровать по видимости: " +
                     "ALL (все), PUBLIC (только публичные), PRIVATE (только приватные). " +
                     "Приватные стикерсеты доступны только автору и администратору."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список стикерсетов успешно получен",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<PageResponse<StickerSetDto>> getStickerSetsByAuthor(
            @Parameter(description = "ID автора (Telegram User ID)", required = true, example = "123456789")
            @PathVariable @Positive Long authorId,
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
            @Parameter(description = "Фильтр по типу стикерсета (USER, OFFICIAL)", example = "USER")
            @RequestParam(required = false) StickerSetType type,
            @Parameter(description = "Фильтр видимости: ALL (все), PUBLIC (только публичные), PRIVATE (только приватные)", example = "ALL")
            @RequestParam(defaultValue = "ALL") VisibilityFilter visibility,
            @Parameter(description = "Вернуть только локальную информацию без telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean shortInfo,
            @Parameter(description = "Режим превью: возвращать только 1 случайный стикер в telegramStickerSetInfo", example = "false")
            @RequestParam(defaultValue = "false") boolean preview,
            HttpServletRequest request) {
        try {
            // Проверка авторизации
            Long currentUserId = helper.getCurrentUserIdOrNull();
            if (currentUserId == null) {
                LOGGER.warn("⚠️ Попытка доступа к стикерсетам автора без авторизации");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Определяем итоговый фильтр видимости
            VisibilityFilter effectiveVisibility = visibility;
            
            // Если пользователь не автор и не админ, принудительно ограничиваем видимость
            if (!helper.isOwnerOrAdmin(authorId, currentUserId)) {
                // Для чужих стикерсетов можем показывать только публичные
                if (visibility == VisibilityFilter.ALL || 
                    visibility == VisibilityFilter.PRIVATE) {
                    effectiveVisibility = VisibilityFilter.PUBLIC;
                    LOGGER.debug("🔒 Пользователь {} не автор/админ для authorId {}, фильтр изменен на PUBLIC", 
                        currentUserId, authorId);
                }
            }
            
            LOGGER.debug("✍️ Получение стикерсетов автора {}: visibility={}, effectiveVisibility={}", 
                authorId, visibility, effectiveVisibility);
            
            // Построение параметров запроса
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(size);
            pageRequest.setSort(sort);
            pageRequest.setDirection(direction);
            
            Set<String> categoryKeysSet = null;
            if (categoryKeys != null && !categoryKeys.trim().isEmpty()) {
                categoryKeysSet = Set.of(categoryKeys.split(","));
            }
            
            String language = helper.getLanguageFromHeaderOrUser(request);
            
            // Вызов сервиса
            PageResponse<StickerSetDto> result = stickerSetService.findByAuthorIdWithPagination(
                authorId,
                pageRequest,
                categoryKeysSet,
                currentUserId,
                effectiveVisibility,
                type,
                shortInfo,
                preview,
                language
            );
            
            LOGGER.debug("✅ Найдено {} стикерсетов автора {} на странице {} из {}", 
                result.getContent().size(), authorId, result.getPage() + 1, result.getTotalPages());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении стикерсетов автора {}: {}", authorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить статистику по стикерсетам
     */
    @GetMapping("/statistics")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Получить статистику по стикерсетам",
        description = "Возвращает статистику по стикерсетам: общее количество, созданные за день/неделю, с разделением на публичные и приватные"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статистика получена",
            content = @Content(schema = @Schema(implementation = StickerSetStatisticsDto.class),
                examples = @ExampleObject(value = """
                    {
                        "total": 5432,
                        "totalPublic": 3200,
                        "totalPrivate": 2232,
                        "daily": 25,
                        "dailyPublic": 15,
                        "dailyPrivate": 10,
                        "weekly": 180,
                        "weeklyPublic": 110,
                        "weeklyPrivate": 70
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<StickerSetStatisticsDto> getStickerSetStatistics() {
        try {
            LOGGER.debug("📊 Запрос статистики по стикерсетам");
            StickerSetStatisticsDto statistics = statisticsService.getStickerSetStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении статистики стикерсетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
