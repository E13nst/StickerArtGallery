package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.CategoryDto;
import com.example.sticker_art_gallery.dto.CreateCategoryDto;
import com.example.sticker_art_gallery.dto.CategoryWithCountDto;
import com.example.sticker_art_gallery.dto.UpdateCategoryDto;
import com.example.sticker_art_gallery.service.category.CategoryService;
import com.example.sticker_art_gallery.service.user.UserService;
import com.example.sticker_art_gallery.model.user.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * REST контроллер для управления категориями стикерсетов
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API для управления категориями стикерсетов")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping
    @Operation(
        summary = "Получить все активные категории",
        description = "Возвращает список всех активных категорий с локализацией через заголовок X-Language (ru/en) или автоматически из initData пользователя. " +
                     "Категории возвращаются отсортированными по displayOrder. " +
                     "Поддерживает русский и английский языки для названий и описаний."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список категорий успешно получен",
            content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<CategoryDto>> getAllCategories(HttpServletRequest request) {
        String language = getLanguageFromHeaderOrUser(request);
        List<CategoryDto> categories = categoryService.getAllActiveCategories(language);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/counts")
    @Operation(
        summary = "Получить активные категории с количеством стикерсетов",
        description = "Возвращает список активных категорий с количеством публичных и не заблокированных стикерсетов в каждой. " +
                     "Поддерживает фильтры: officialOnly, authorId, hasAuthorOnly."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список категорий с количеством успешно получен",
            content = @Content(schema = @Schema(implementation = CategoryWithCountDto.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<CategoryWithCountDto>> getCategoriesWithCounts(
            @Parameter(description = "Показывать только официальные стикерсеты", example = "false")
            @RequestParam(defaultValue = "false") boolean officialOnly,
            @Parameter(description = "Фильтр по автору (Telegram ID)", example = "123456789")
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Показывать только авторские стикерсеты (authorId IS NOT NULL)", example = "false")
            @RequestParam(defaultValue = "false") boolean hasAuthorOnly,
            HttpServletRequest request) {
        String language = getLanguageFromHeaderOrUser(request);
        List<CategoryWithCountDto> result = categoryService.getActiveCategoriesWithCounts(language, officialOnly, authorId, hasAuthorOnly);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Получить категорию по ключу",
        description = "Возвращает информацию о категории по её уникальному ключу. " +
                     "Ключ должен содержать только латинские буквы, цифры и подчеркивания. " +
                     "Поддерживает локализацию названий и описаний через заголовок X-Language (ru/en) или автоматически из initData пользователя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Категория найдена",
            content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<CategoryDto> getCategoryByKey(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key,
            HttpServletRequest request
    ) {
        try {
            String language = getLanguageFromHeaderOrUser(request);
            CategoryDto category = categoryService.getCategoryByKey(key, language);
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(
        summary = "Создать новую категорию",
        description = "Создает новую категорию с поддержкой локализации. " +
                     "Поле 'key' является обязательным и должно быть уникальным. " +
                     "Поддерживает создание категорий с названиями и описаниями на русском и английском языках. " +
                     "Язык ответа определяется через заголовок X-Language (ru/en) или автоматически из initData пользователя. " +
                     "displayOrder определяет порядок отображения в списке категорий."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Категория успешно создана",
            content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные или категория уже существует"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> createCategory(
            @Valid @RequestBody CreateCategoryDto createDto,
            HttpServletRequest request
    ) {
        try {
            String language = getLanguageFromHeaderOrUser(request);
            CategoryDto category = categoryService.createCategory(createDto, language);
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{key}")
    @Operation(
        summary = "Обновить категорию",
        description = "Обновляет существующую категорию. Требуется авторизация. " +
                     "Язык ответа определяется через заголовок X-Language (ru/en) или автоматически из initData пользователя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Категория успешно обновлена"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> updateCategory(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key,
            @Valid @RequestBody UpdateCategoryDto updateDto,
            HttpServletRequest request
    ) {
        try {
            String language = getLanguageFromHeaderOrUser(request);
            CategoryDto category = categoryService.updateCategory(key, updateDto, language);
            return ResponseEntity.ok(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{key}")
    @Operation(
        summary = "Деактивировать категорию",
        description = "Деактивирует категорию (мягкое удаление). Требуется авторизация."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Категория успешно деактивирована"),
        @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> deactivateCategory(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key
    ) {
        try {
            categoryService.deactivateCategory(key);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{key}/count")
    @Operation(
        summary = "Получить количество стикерсетов в категории",
        description = "Возвращает количество стикерсетов, привязанных к категории"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Количество успешно получено"),
        @ApiResponse(responseCode = "404", description = "Категория не найдена"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getStickerSetCount(
            @Parameter(description = "Уникальный ключ категории", example = "animals")
            @PathVariable String key
    ) {
        try {
            long count = categoryService.getStickerSetCount(key);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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
                            return lang;
                        }
                    }
                }
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }
        
        // По умолчанию возвращаем английский
        return "en";
    }
}

