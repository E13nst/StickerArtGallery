package com.example.sticker_art_gallery.controller.stylefeed;

import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedItemDto;
import com.example.sticker_art_gallery.model.stylefeed.CandidateFeedVisibility;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/admin/style-feed/items", "/api/admin/style-feed/items"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Style Feed", description = "Управление записами ленты style feed")
public class AdminStyleFeedItemController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminStyleFeedItemController.class);

    private final StyleFeedItemService styleFeedItemService;

    public AdminStyleFeedItemController(StyleFeedItemService styleFeedItemService) {
        this.styleFeedItemService = styleFeedItemService;
    }

    @GetMapping
    @Operation(summary = "Список записей style feed")
    public ResponseEntity<List<StyleFeedItemDto>> list(
            @RequestParam(required = false) CandidateFeedVisibility visibility) {
        return ResponseEntity.ok(styleFeedItemService.listForAdmin(Optional.ofNullable(visibility)));
    }

    @PostMapping("/{id}/visibility")
    @Operation(summary = "Admin-override видимости (SHOW / HIDE / RESET)")
    public ResponseEntity<StyleFeedItemDto> setVisibility(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String action = body.get("override");
            if (action == null || action.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            StyleFeedItemDto result = styleFeedItemService.setAdminVisibilityOverride(id, action);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка видимости {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка видимости {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Заменить preview")
    public ResponseEntity<StyleFeedItemDto> replacePreview(@PathVariable Long id,
                                                          @RequestParam("file") MultipartFile file) {
        try {
            StyleFeedItemDto result = styleFeedItemService.replacePreview(id, file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.warn("Ошибка preview {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка preview {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
