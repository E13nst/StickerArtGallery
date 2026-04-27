package com.example.sticker_art_gallery.controller.generation;

import com.example.sticker_art_gallery.dto.generation.PresetPublicationRequestDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.service.generation.StylePresetPublicationService;
import com.example.sticker_art_gallery.service.generation.StylePresetService;
import com.example.sticker_art_gallery.service.generation.UserPresetLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Расширения API стиль-пресетов:
 * - Публикация пользовательского пресета (с idempotency)
 * - Сохранённые (лайкнутые) пресеты
 * - Загрузка reference-изображения владельцем
 */
@RestController
@RequestMapping("/api/style-presets")
@Tag(name = "Style Presets (ext)", description = "Публикация, сохранённые пресеты, reference-загрузка")
public class StylePresetExtController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetExtController.class);

    private final StylePresetPublicationService publicationService;
    private final UserPresetLikeService userPresetLikeService;
    private final StylePresetService stylePresetService;

    public StylePresetExtController(StylePresetPublicationService publicationService,
                                     UserPresetLikeService userPresetLikeService,
                                     StylePresetService stylePresetService) {
        this.publicationService = publicationService;
        this.userPresetLikeService = userPresetLikeService;
        this.stylePresetService = stylePresetService;
    }

    // =========================================================================
    // Публикация
    // =========================================================================

    @PostMapping("/{presetId}/publish")
    @Operation(
            summary = "Опубликовать пользовательский пресет",
            description = "Списывает 10 ART и переводит пресет в статус PENDING_MODERATION. " +
                          "Идемпотентен — повторный запрос с тем же idempotencyKey не списывает ART дважды.")
    public ResponseEntity<StylePresetDto> publishPreset(
            @PathVariable Long presetId,
            @RequestBody PresetPublicationRequestDto request) {
        try {
            Long userId = getCurrentUserId();
            StylePresetDto result = publicationService.publishPreset(userId, presetId, request.getIdempotencyKey());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("не авторизован")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            LOGGER.warn("Ошибка публикации пресета {}: {}", presetId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка публикации пресета {}: {}", presetId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Неожиданная ошибка публикации пресета {}: {}", presetId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Сохранённые пресеты
    // =========================================================================

    @GetMapping("/liked")
    @Operation(
            summary = "Список сохранённых пресетов текущего пользователя",
            description = "Виртуальная категория — возвращает пресеты из user_preset_likes")
    public ResponseEntity<List<StylePresetDto>> getLikedPresets() {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(userPresetLikeService.getLikedPresets(userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            LOGGER.error("Ошибка получения сохранённых пресетов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{presetId}/like")
    @Operation(summary = "Добавить пресет в сохранённые")
    public ResponseEntity<Void> likePreset(@PathVariable Long presetId) {
        try {
            Long userId = getCurrentUserId();
            userPresetLikeService.likePreset(userId, presetId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка добавления пресета {} в сохранённые: {}", presetId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{presetId}/like")
    @Operation(summary = "Убрать пресет из сохранённых")
    public ResponseEntity<Void> unlikePreset(@PathVariable Long presetId) {
        try {
            Long userId = getCurrentUserId();
            userPresetLikeService.unlikePreset(userId, presetId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Reference-изображение для владельца
    // =========================================================================

    @PutMapping(value = "/{presetId}/reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Загрузить reference-изображение (только владелец пресета)",
            description = "User-safe путь. Аналог admin-пути uploadReferenceForGlobal, но доступен владельцу. " +
                          "Поддерживаемые форматы: image/png, image/webp, image/jpeg. Максимальный размер: 3MB.")
    public ResponseEntity<StylePresetDto> uploadReference(
            @PathVariable Long presetId,
            @Parameter(description = "Файл референс-изображения")
            @RequestParam("file") MultipartFile file) {
        try {
            Long userId = getCurrentUserId();
            StylePresetDto result = stylePresetService.uploadReferenceForOwner(presetId, userId, file);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка загрузки reference для пресета {}: {}", presetId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при загрузке reference: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Admin: модерация
    // =========================================================================

    @PostMapping("/admin/{presetId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: установить статус модерации пресета",
            description = "Переводит пресет из PENDING_MODERATION в APPROVED или REJECTED. " +
                          "Body: { \"status\": \"APPROVED\" | \"REJECTED\" }")
    public ResponseEntity<StylePresetDto> moderatePreset(
            @PathVariable Long presetId,
            @RequestBody Map<String, String> body) {
        try {
            String statusStr = body.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().build();
            }
            PresetModerationStatus status = PresetModerationStatus.valueOf(statusStr.toUpperCase());
            StylePresetDto result = publicationService.moderatePreset(presetId, status);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка модерации пресета {}: {}", presetId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            LOGGER.warn("Ошибка состояния модерации пресета {}: {}", presetId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка модерации: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        String name = auth.getName();
        try {
            if (name.matches("\\d+")) {
                return Long.parseLong(name);
            }
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalStateException("Не удалось определить userId из аутентификации");
    }
}
