package com.example.sticker_art_gallery.controller.meme;

import com.example.sticker_art_gallery.dto.meme.MemeCandidateDto;
import com.example.sticker_art_gallery.service.meme.MemeCandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Admin-эндпоинты для управления видимостью мем-кандидатов.
 */
@RestController
@RequestMapping({"/admin/meme-candidates", "/api/admin/meme-candidates"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Meme Candidates", description = "Управление видимостью мем-кандидатов")
public class AdminMemeCandidateController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminMemeCandidateController.class);

    private final MemeCandidateService memeCandidateService;

    public AdminMemeCandidateController(MemeCandidateService memeCandidateService) {
        this.memeCandidateService = memeCandidateService;
    }

    /**
     * Установить admin-override видимости.
     * Body: { "override": "SHOW" | "HIDE" | "RESET" }
     * SHOW  → ADMIN_FORCED_VISIBLE (кандидат всегда виден в ленте)
     * HIDE  → ADMIN_HIDDEN (кандидат скрыт принудительно)
     * RESET → убирает override, возвращает к автоправилу
     */
    @PostMapping("/{id}/visibility")
    @Operation(
            summary = "Установить admin-override видимости кандидата",
            description = "override: SHOW — принудительно показать; HIDE — скрыть; RESET — вернуть к автоправилу")
    public ResponseEntity<MemeCandidateDto> setVisibility(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String action = body.get("override");
            if (action == null || action.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            MemeCandidateDto result = memeCandidateService.setAdminVisibilityOverride(id, action);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка установки видимости кандидата {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при установке видимости кандидата {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Заменить preview-изображение у meme-candidate")
    public ResponseEntity<MemeCandidateDto> replacePreview(@PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file) {
        try {
            MemeCandidateDto result = memeCandidateService.replacePreview(id, file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.warn("Ошибка замены preview у кандидата {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка замены preview у кандидата {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
