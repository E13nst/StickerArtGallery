package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.CategoryDto;
import com.example.sticker_art_gallery.dto.CreateCategoryDto;
import com.example.sticker_art_gallery.dto.UpdateCategoryDto;
import com.example.sticker_art_gallery.service.category.CategoryService;
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
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @Operation(
        summary = "Получить все активные категории",
        description = "Возвращает список всех активных категорий с локализацией в зависимости от параметра language. " +
                     "Категории возвращаются отсортированными по displayOrder. " +
                     "Поддерживает русский и английский языки для названий и описаний."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список категорий успешно получен",
            content = @Content(schema = @Schema(implementation = CategoryDto.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @Parameter(description = "Код языка для локализации (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language
    ) {
        List<CategoryDto> categories = categoryService.getAllActiveCategories(language);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Получить категорию по ключу",
        description = "Возвращает информацию о категории по её уникальному ключу. " +
                     "Ключ должен содержать только латинские буквы, цифры и подчеркивания. " +
                     "Поддерживает локализацию названий и описаний."
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
            @Parameter(description = "Код языка для локализации (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language
    ) {
        try {
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
            @Parameter(description = "Код языка для ответа (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language
    ) {
        try {
            CategoryDto category = categoryService.createCategory(createDto, language);
            return ResponseEntity.status(HttpStatus.CREATED).body(category);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{key}")
    @Operation(
        summary = "Обновить категорию",
        description = "Обновляет существующую категорию. Требуется авторизация."
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
            @Parameter(description = "Код языка для ответа (ru/en)", example = "ru")
            @RequestParam(defaultValue = "en") String language
    ) {
        try {
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
}

