package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetModerationStatsDto;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.service.generation.StylePresetPublicationService;
import com.example.sticker_art_gallery.service.generation.StylePresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Админ API: модерация пользовательских пресетов, статистика, просмотр референсов.
 */
@RestController
@RequestMapping("/api/admin/style-presets/moderation")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: модерация пресетов", description = "Список пользовательских пресетов, статистика, решения по модерации")
@SecurityRequirement(name = "TelegramInitData")
public class StylePresetModerationAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetModerationAdminController.class);

    private final StylePresetService stylePresetService;
    private final StylePresetPublicationService publicationService;

    public StylePresetModerationAdminController(StylePresetService stylePresetService,
                                              StylePresetPublicationService publicationService) {
        this.stylePresetService = stylePresetService;
        this.publicationService = publicationService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Статистика по пользовательским пресетам")
    public ResponseEntity<StylePresetModerationStatsDto> getStats() {
        return ResponseEntity.ok(stylePresetService.getModerationStats());
    }

    @GetMapping
    @Operation(summary = "Список пользовательских пресетов",
            description = "Включает previewUrl и presetReferenceImageUrl для просмотра в админке")
    public ResponseEntity<List<StylePresetDto>> list(
            @Parameter(description = "Фильтр по статусу модерации; без параметра — все")
            @RequestParam(name = "status", required = false) PresetModerationStatus status) {
        List<StylePresetDto> list = stylePresetService.listUserPresetsForAdmin(status);
        LOGGER.debug("Admin: {} user presets (filter={})", list.size(), status);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{presetId}")
    @Operation(summary = "Карточка пользовательского пресета (детально)")
    public ResponseEntity<StylePresetDto> getOne(@PathVariable Long presetId) {
        StylePresetDto dto = stylePresetService.getPresetById(presetId, null);
        if (Boolean.TRUE.equals(dto.getIsGlobal())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{presetId}/decide")
    @Operation(summary = "Принять решение по модерации",
            description = "Только из статуса PENDING_MODERATION. Body: { \"status\": \"APPROVED\" | \"REJECTED\" }")
    public ResponseEntity<StylePresetDto> decide(
            @PathVariable Long presetId,
            @RequestBody Map<String, String> body) {
        String statusStr = body != null ? body.get("status") : null;
        if (statusStr == null) {
            return ResponseEntity.badRequest().build();
        }
        PresetModerationStatus newStatus = PresetModerationStatus.valueOf(statusStr.toUpperCase());
        StylePresetDto result = publicationService.moderatePreset(presetId, newStatus);
        return ResponseEntity.ok(result);
    }
}
