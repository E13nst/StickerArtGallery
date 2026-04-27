package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetCategoryRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetCategoryDto;
import com.example.sticker_art_gallery.dto.generation.UpdateStylePresetCategoryRequest;
import com.example.sticker_art_gallery.service.generation.StylePresetCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/generation/style-preset-categories")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Категории стилей", description = "Справочник категорий для пресетов стилей")
@SecurityRequirement(name = "TelegramInitData")
public class StylePresetCategoryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetCategoryController.class);

    private final StylePresetCategoryService categoryService;

    @Autowired
    public StylePresetCategoryController(StylePresetCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Список категорий стилей", description = "Упорядочено по sortOrder, name")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = StylePresetCategoryDto.class)))
    public ResponseEntity<List<StylePresetCategoryDto>> list() {
        List<StylePresetCategoryDto> list = categoryService.listAllOrdered();
        LOGGER.debug("Returning {} style preset categories", list.size());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать категорию", description = "Только админ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Создана",
                    content = @Content(schema = @Schema(implementation = StylePresetCategoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные или дубликат code")
    })
    public ResponseEntity<StylePresetCategoryDto> create(@Valid @RequestBody CreateStylePresetCategoryRequest request) {
        try {
            return ResponseEntity.ok(categoryService.create(request));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Create category failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить категорию", description = "Только name и sortOrder; code не меняется")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = StylePresetCategoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    public ResponseEntity<StylePresetCategoryDto> update(
            @Parameter(description = "ID категории", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateStylePresetCategoryRequest request) {
        try {
            return ResponseEntity.ok(categoryService.update(id, request));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Category not found")) {
                return ResponseEntity.notFound().build();
            }
            LOGGER.warn("Update category failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить категорию", description = "Пресеты переносятся в general; удалить general нельзя")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалена"),
            @ApiResponse(responseCode = "400", description = "Нельзя удалить или некорректный запрос"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID категории", required = true) @PathVariable Long id) {
        try {
            categoryService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Category not found")) {
                return ResponseEntity.notFound().build();
            }
            LOGGER.warn("Delete category failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
