package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetListView;
import com.example.sticker_art_gallery.service.generation.StylePresetListEtag;
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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/generation/style-presets")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Пресеты стилей", description = "API для управления пресетами стилей генерации (пользовательские и глобальные)")
@SecurityRequirement(name = "TelegramInitData")
public class StylePresetController {

    private static final CacheControl PRESET_LIST_CACHE_CONTROL = CacheControl.maxAge(Duration.ofSeconds(60));

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetController.class);

    private final StylePresetService presetService;

    @Autowired
    public StylePresetController(StylePresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    @Operation(
        summary = "Получить доступные пресеты",
        description = """
                Список доступных пресетов (глобальные + персональные + опубликованные в каталоге).
                Параметр view задаёт проекцию: browse — витрина (превью без полной UI-схемы),
                generation — экран генерации (без протяжки серверного референса для чужих и глобальных пресетов),
                full — полный DTO. Если view не указан: includeUi=true эквивалентно full, includeUi=false — только метаданные.
                Для browse и режима только метаданных возвращается weak ETag и короткий Cache-Control для условных запросов."""
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список пресетов получен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "304", description = "Не изменено (If-None-Match совпал с ETag)")
    })
    public ResponseEntity<List<StylePresetDto>> getAvailablePresets(
            WebRequest webRequest,
            @Parameter(description = "Legacy: при отсутствии view — false даёт только метаданные, true — полный ответ")
            @RequestParam(name = "includeUi", defaultValue = "false") boolean includeUi,
            @Parameter(description = "browse | generation | full")
            @RequestParam(name = "view", required = false) StylePresetListView view) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<StylePresetDto> presets = presetService.getAvailablePresets(userId, includeUi, isCurrentUserAdmin(), view);
        LOGGER.info("Returning {} available presets for user {} includeUi={} view={}", presets.size(), userId, includeUi, view);

        if (presetService.availablePresetsListSupportsWeakEtag(view, includeUi)) {
            String etag = StylePresetListEtag.weakHexDigest(presets);
            if (webRequest.checkNotModified(etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .cacheControl(PRESET_LIST_CACHE_CONTROL)
                        .build();
            }
            return ResponseEntity.ok()
                    .eTag(etag)
                    .cacheControl(PRESET_LIST_CACHE_CONTROL)
                    .body(presets);
        }
        return ResponseEntity.ok(presets);
    }

    @PostMapping(value = "/{id}/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Загрузить превью (PNG/JPEG/WebP) для глобального пресета", description = "Одна картинка; прежний файл заменяется")
    public ResponseEntity<StylePresetDto> uploadPreview(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(presetService.uploadPreviewForGlobal(id, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/{id}/reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Загрузить референс (PNG/JPEG/WebP) для глобального пресета", description = "Подстановка в слот reference и в img_sagref_*; прежний файл заменяется")
    public ResponseEntity<StylePresetDto> uploadReference(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(presetService.uploadReferenceForGlobal(id, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/reference")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить референсное изображение пресета", description = "Только глобальные пресеты")
    public ResponseEntity<StylePresetDto> clearReference(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(presetService.clearReferenceForGlobal(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
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

    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Получить все глобальные пресеты",
        description = "Возвращает список всех глобальных пресетов (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список глобальных пресетов получен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public ResponseEntity<List<StylePresetDto>> getAllGlobalPresets() {
        List<StylePresetDto> presets = presetService.getAllGlobalPresets();
        LOGGER.info("Returning {} global presets", presets.size());
        return ResponseEntity.ok(presets);
    }

    @PostMapping
    @Operation(
        summary = "Создать персональный пресет (create-or-get)",
        description = """
                Создаёт черновик персонального пресета для текущего пользователя.
                Идемпотентность по паре (владелец, code): если пресет с тем же code уже есть у этого пользователя,
                возвращается существующая запись с тем же id (повторные вызовы из ретраев/восстановления сессии — без 400).
                Пара (owner, code) уникальна в БД; у другого пользователя тот же code — отдельный пресет.
                Код в теле запроса нормализуется обрезкой пробелов по краям.
                """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пресет создан или найден существующий с тем же code у владельца",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Неверные входные данные (валидация тела запроса)")
    })
    public ResponseEntity<StylePresetDto> createPreset(@Valid @RequestBody CreateStylePresetRequest request) {
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StylePresetDto preset = presetService.createUserPreset(userId, request);
            LOGGER.info("Upsert preset: id={}, code={}, userId={}", preset.getId(), preset.getCode(), userId);
            return ResponseEntity.ok(preset);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to create preset: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
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
        @ApiResponse(responseCode = "400", description = "Неверные входные данные или пресет с таким кодом уже существует"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен")
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
        summary = "Обновить пресет",
        description = "Обновляет пресет (пользователь может обновлять только свои пресеты, админ - любые)"
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

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Включить/выключить пресет",
        description = "Включает или выключает пресет (только для админа)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статус пресета изменен",
            content = @Content(schema = @Schema(implementation = StylePresetDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
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

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить пресет",
        description = "Удаляет пресет. Владелец может удалить только свой неглобальный пресет, в том числе одобренный и опубликованный в каталоге (независимо от moderationStatus и publishedToCatalog). Админ может удалить любой пресет."
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
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking admin role: {}", e.getMessage());
        }
        return false;
    }
}
