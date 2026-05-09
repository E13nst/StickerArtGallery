package com.example.sticker_art_gallery.controller.stylefeed;

import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedQaRepeatRatedStateDto;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedQaSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({
        "/admin/style-feed/qa-repeat-rated",
        "/api/admin/style-feed/qa-repeat-rated"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Style feed QA", description = "QA-режим повторной выдачи оценённых карточек style feed")
public class AdminStyleFeedQaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminStyleFeedQaController.class);

    private final StyleFeedQaSettingsService styleFeedQaSettingsService;

    public AdminStyleFeedQaController(StyleFeedQaSettingsService styleFeedQaSettingsService) {
        this.styleFeedQaSettingsService = styleFeedQaSettingsService;
    }

    @GetMapping
    @Operation(summary = "Текущее состояние QA repeat-rated и источники (БД / env)")
    public ResponseEntity<StyleFeedQaRepeatRatedStateDto> getState() {
        return ResponseEntity.ok(styleFeedQaSettingsService.getStateForAdmin());
    }

    /**
     * {@code telegramUserId: null} + {@code clearDatabase: true} — убрать запись из БД (действует env).<br>
     * {@code telegramUserId: 0} — явно выключить QA и перекрыть env.<br>
     * {@code telegramUserId > 0} — включить для этого Telegram user id.
     */
    @PutMapping
    @Operation(summary = "Задать telegram user id для QA или снять переопределение из БД")
    public ResponseEntity<?> put(@RequestBody Map<String, Object> body) {
        try {
            if (body != null && Boolean.TRUE.equals(body.get("clearDatabase"))) {
                LOGGER.info("admin: снято переопределение style-feed QA из БД");
                return ResponseEntity.ok(styleFeedQaSettingsService.saveRepeatRatedTelegramUserId(null));
            }
            if (body == null || !body.containsKey("telegramUserId")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Укажите telegramUserId (число) или clearDatabase: true"));
            }
            Object raw = body.get("telegramUserId");
            if (raw == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error",
                        "Для снятия переопределения отправьте { \"clearDatabase\": true }"));
            }
            long userId;
            if (raw instanceof Number n) {
                userId = n.longValue();
            } else {
                userId = Long.parseLong(raw.toString());
            }
            return ResponseEntity.ok(styleFeedQaSettingsService.saveRepeatRatedTelegramUserId(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("admin style-feed QA PUT: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
