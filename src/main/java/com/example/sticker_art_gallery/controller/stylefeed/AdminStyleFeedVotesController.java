package com.example.sticker_art_gallery.controller.stylefeed;

import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedVotesResetResultDto;
import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedVotesStatsDto;
import com.example.sticker_art_gallery.security.AuthUserPrincipal;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedVotesAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Статистика и безопасный сброс голосов {@code style_feed_item_likes/dislikes} для тестирования ленты.
 */
@RestController
@RequestMapping({"/admin/style-feed/votes", "/api/admin/style-feed/votes"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Style Feed Votes", description = "Статистика и сброс лайков/дизлайков ленты style feed")
public class AdminStyleFeedVotesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminStyleFeedVotesController.class);

    private final StyleFeedVotesAdminService styleFeedVotesAdminService;

    public AdminStyleFeedVotesController(StyleFeedVotesAdminService styleFeedVotesAdminService) {
        this.styleFeedVotesAdminService = styleFeedVotesAdminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Сводка по голосам style feed")
    public ResponseEntity<StyleFeedVotesStatsDto> stats() {
        return ResponseEntity.ok(styleFeedVotesAdminService.getStats());
    }

    @PostMapping("/reset-for-me")
    @Operation(summary = "Сбросить голоса style feed только для текущего админа (user id из Telegram-сессии)")
    public ResponseEntity<?> resetForCurrentAdmin() {
        try {
            long userId = requireCurrentPrincipalUserId();
            StyleFeedVotesResetResultDto result = styleFeedVotesAdminService.resetAllStyleFeedVotesForUser(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            LOGGER.warn("reset-for-me: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("reset-for-me failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/reset-for-user")
    @Operation(summary = "Сбросить лайки/дизлайки style feed для пользователя (Telegram user id = users.id)")
    public ResponseEntity<?> resetForUser(@RequestBody Map<String, Object> body) {
        try {
            Object raw = body != null ? body.get("userId") : null;
            if (raw == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Требуется userId (Telegram ID пользователя)"));
            }
            long userId;
            if (raw instanceof Number n) {
                userId = n.longValue();
            } else {
                userId = Long.parseLong(raw.toString());
            }
            if (userId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Некорректный userId"));
            }
            StyleFeedVotesResetResultDto result = styleFeedVotesAdminService.resetAllStyleFeedVotesForUser(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("reset style feed votes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("reset style feed votes failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static long requireCurrentPrincipalUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Нет авторизации");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthUserPrincipal p) {
            return p.getUserId();
        }
        throw new IllegalStateException("Ожидался профиль Telegram (войдите через initData в админке)");
    }
}
