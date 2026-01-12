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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/generation/style-presets")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Пресеты стилей", description = "API для управления пресетами стилей генерации")
@SecurityRequirement(name = "TelegramInitData")
public class StylePresetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetController.class);

    private final StylePresetService presetService;

    @Autowired
    public StylePresetController(StylePresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    @Operation(
        summary = "Получить доступные пресеты",
        description = "Возвращает список всех доступных пресетов для пользователя (глобальные + персональные)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список пресетов получен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        )
    })
    public ResponseEntity<List<StylePresetDto>> getAvailablePresets() {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<StylePresetDto> presets = presetService.getAvailablePresets(userId);
        LOGGER.info("Returning {} available presets for user {}", presets.size(), userId);
        return ResponseEntity.ok(presets);
    }

    @GetMapping("/my")
    @Operation(
        summary = "Получить персональные пресеты",
        description = "Возвращает список персональных пресетов пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список персональных пресетов получен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        )
    })
    public ResponseEntity<List<StylePresetDto>> getMyPresets() {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<StylePresetDto> presets = presetService.getUserPresets(userId);
        LOGGER.info("Returning {} personal presets for user {}", presets.size(), userId);
        return ResponseEntity.ok(presets);
    }

    @PostMapping
    @Operation(
        summary = "Создать персональный пресет",
        description = "Создает новый персональный пресет стиля для пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пресет создан",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные или пресет с таким кодом уже существует")
    })
    public ResponseEntity<StylePresetDto> createPreset(@Valid @RequestBody CreateStylePresetRequest request) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StylePresetDto preset = presetService.createUserPreset(userId, request);
            LOGGER.info("Created preset: id={}, code={}, userId={}", preset.getId(), preset.getCode(), userId);
            return ResponseEntity.ok(preset);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create preset: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить пресет",
        description = "Обновляет персональный пресет пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пресет обновлен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "404", description = "Пресет не найден")
    })
    public ResponseEntity<StylePresetDto> updatePreset(
            @Parameter(description = "ID пресета", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CreateStylePresetRequest request) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = isCurrentUserAdmin();
        try {
            StylePresetDto preset = presetService.updatePreset(id, userId, request, isAdmin);
            LOGGER.info("Updated preset: id={}", id);
            return ResponseEntity.ok(preset);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить пресет",
        description = "Удаляет персональный пресет пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пресет удален"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "404", description = "Пресет не найден")
    })
    public ResponseEntity<Void> deletePreset(
            @Parameter(description = "ID пресета", required = true)
            @PathVariable Long id) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = isCurrentUserAdmin();
        try {
            presetService.deletePreset(id, userId, isAdmin);
            LOGGER.info("Deleted preset: id={}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private Long extractUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse userId from authentication name: {}", authentication.getName());
            return null;
        }
    }

    private boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking admin role: {}", e.getMessage());
        }
        return false;
    }
}
