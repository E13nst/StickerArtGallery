package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.CreatePromptEnhancerRequest;
import com.example.sticker_art_gallery.dto.generation.PromptEnhancerDto;
import com.example.sticker_art_gallery.service.generation.PromptEnhancerService;
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
@RequestMapping("/api/admin/prompt-enhancers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Энхансеры промптов (Админ)", description = "API для управления энхансерами промптов (только для админа)")
@SecurityRequirement(name = "TelegramInitData")
public class PromptEnhancerAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptEnhancerAdminController.class);

    private final PromptEnhancerService enhancerService;

    @Autowired
    public PromptEnhancerAdminController(PromptEnhancerService enhancerService) {
        this.enhancerService = enhancerService;
    }

    @GetMapping
    @Operation(
        summary = "Получить все глобальные энхансеры",
        description = "Возвращает список всех глобальных энхансеров промптов (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список глобальных энхансеров получен",
            content = @Content(schema = @Schema(implementation = PromptEnhancerDto.class))
        )
    })
    public ResponseEntity<List<PromptEnhancerDto>> getAllGlobalEnhancers() {
        List<PromptEnhancerDto> enhancers = enhancerService.getAllGlobalEnhancers();
        LOGGER.info("Returning {} global enhancers", enhancers.size());
        return ResponseEntity.ok(enhancers);
    }

    @PostMapping
    @Operation(
        summary = "Создать глобальный энхансер",
        description = "Создает новый глобальный энхансер промптов (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Глобальный энхансер создан",
            content = @Content(schema = @Schema(implementation = PromptEnhancerDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные или энхансер с таким кодом уже существует")
    })
    public ResponseEntity<PromptEnhancerDto> createGlobalEnhancer(@Valid @RequestBody CreatePromptEnhancerRequest request) {
        try {
            PromptEnhancerDto enhancer = enhancerService.createGlobalEnhancer(request);
            LOGGER.info("Created global enhancer: id={}, code={}", enhancer.getId(), enhancer.getCode());
            return ResponseEntity.ok(enhancer);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create global enhancer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить глобальный энхансер",
        description = "Обновляет глобальный энхансер (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Энхансер обновлен",
            content = @Content(schema = @Schema(implementation = PromptEnhancerDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Энхансер не найден")
    })
    public ResponseEntity<PromptEnhancerDto> updateGlobalEnhancer(
            @Parameter(description = "ID энхансера", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CreatePromptEnhancerRequest request) {
        try {
            PromptEnhancerDto enhancer = enhancerService.updateEnhancer(id, request);
            LOGGER.info("Updated global enhancer: id={}", id);
            return ResponseEntity.ok(enhancer);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить глобальный энхансер",
        description = "Удаляет глобальный энхансер (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Энхансер удален"),
        @ApiResponse(responseCode = "404", description = "Энхансер не найден")
    })
    public ResponseEntity<Void> deleteGlobalEnhancer(
            @Parameter(description = "ID энхансера", required = true)
            @PathVariable Long id) {
        try {
            enhancerService.deleteEnhancer(id);
            LOGGER.info("Deleted global enhancer: id={}", id);
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
        summary = "Включить/выключить энхансер",
        description = "Включает или выключает глобальный энхансер (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статус энхансера изменен",
            content = @Content(schema = @Schema(implementation = PromptEnhancerDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Энхансер не найден")
    })
    public ResponseEntity<PromptEnhancerDto> toggleEnhancerEnabled(
            @Parameter(description = "ID энхансера", required = true)
            @PathVariable Long id,
            @Parameter(description = "Включить энхансер", example = "true")
            @RequestParam boolean enabled) {
        try {
            PromptEnhancerDto enhancer = enhancerService.toggleEnhancerEnabled(id, enabled);
            LOGGER.info("Toggled enhancer enabled: id={}, enabled={}", id, enabled);
            return ResponseEntity.ok(enhancer);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
