package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.service.generation.StylePresetService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/style-presets")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Пресеты стилей (Админ)", description = "API для управления глобальными пресетами стилей")
@SecurityRequirement(name = "TelegramInitData")
public class StylePresetAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetAdminController.class);

    private final StylePresetService presetService;

    @Autowired
    public StylePresetAdminController(StylePresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    @Operation(
        summary = "Получить все глобальные пресеты",
        description = "Возвращает список всех глобальных пресетов (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список глобальных пресетов получен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        )
    })
    public ResponseEntity<List<StylePresetDto>> getAllGlobalPresets() {
        List<StylePresetDto> presets = presetService.getAllGlobalPresets();
        LOGGER.info("Returning {} global presets", presets.size());
        return ResponseEntity.ok(presets);
    }

    @PostMapping
    @Operation(
        summary = "Создать глобальный пресет",
        description = "Создает новый глобальный пресет стиля (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Глобальный пресет создан",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные или пресет с таким кодом уже существует")
    })
    public ResponseEntity<StylePresetDto> createGlobalPreset(@Valid @RequestBody CreateStylePresetRequest request) {
        try {
            StylePresetDto preset = presetService.createGlobalPreset(request);
            LOGGER.info("Created global preset: id={}, code={}", preset.getId(), preset.getCode());
            return ResponseEntity.ok(preset);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create global preset: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить глобальный пресет",
        description = "Обновляет глобальный пресет (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пресет обновлен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Пресет не найден")
    })
    public ResponseEntity<StylePresetDto> updateGlobalPreset(
            @Parameter(description = "ID пресета", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CreateStylePresetRequest request) {
        try {
            // Для админа userId не важен, но передаем null для глобальных пресетов
            StylePresetDto preset = presetService.updatePreset(id, null, request, true);
            LOGGER.info("Updated global preset: id={}", id);
            return ResponseEntity.ok(preset);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить глобальный пресет",
        description = "Удаляет глобальный пресет (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пресет удален"),
        @ApiResponse(responseCode = "404", description = "Пресет не найден")
    })
    public ResponseEntity<Void> deleteGlobalPreset(
            @Parameter(description = "ID пресета", required = true)
            @PathVariable Long id) {
        try {
            presetService.deletePreset(id, null, true);
            LOGGER.info("Deleted global preset: id={}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}/toggle")
    @Operation(
        summary = "Включить/выключить пресет",
        description = "Включает или выключает глобальный пресет (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статус пресета изменен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Пресет не найден")
    })
    public ResponseEntity<StylePresetDto> togglePresetEnabled(
            @Parameter(description = "ID пресета", required = true)
            @PathVariable Long id,
            @Parameter(description = "Включить пресет", example = "true")
            @RequestParam boolean enabled) {
        try {
            StylePresetDto preset = presetService.togglePresetEnabled(id, enabled);
            LOGGER.info("Toggled preset enabled: id={}, enabled={}", id, enabled);
            return ResponseEntity.ok(preset);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
