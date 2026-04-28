package com.example.sticker_art_gallery.controller.stylefeed;

import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedItemDto;
import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedItemVoteResponseDto;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/style-feed")
@Tag(name = "Style Feed", description = "Лента style feed — оценка пресетов (лайки/дизлайки)")
public class StyleFeedItemController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFeedItemController.class);

    private final StyleFeedItemService styleFeedItemService;

    public StyleFeedItemController(StyleFeedItemService styleFeedItemService) {
        this.styleFeedItemService = styleFeedItemService;
    }

    @GetMapping("/feed/next")
    @Operation(summary = "Следующая запись из ленты")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запись найдена"),
            @ApiResponse(responseCode = "204", description = "Нет доступных записей"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "429", description = "Лимит свайпов")
    })
    public ResponseEntity<StyleFeedItemDto> getNext() {
        try {
            Long userId = getCurrentUserId();
            return styleFeedItemService.getNextForFeed(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (com.example.sticker_art_gallery.exception.SwipeLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при получении style feed item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{itemId}/like")
    @Operation(summary = "Лайк")
    public ResponseEntity<StyleFeedItemVoteResponseDto> like(
            @PathVariable Long itemId,
            @Parameter(description = "Свайп: лимит и награды", example = "false")
            @RequestParam(defaultValue = "false") boolean isSwipe) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(styleFeedItemService.likeFeedItem(userId, itemId, isSwipe));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка лайка style feed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка лайка {}: {}", itemId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{itemId}/like")
    @Operation(summary = "Снять лайк")
    public ResponseEntity<StyleFeedItemVoteResponseDto> unlike(@PathVariable Long itemId) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(styleFeedItemService.unlikeFeedItem(userId, itemId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка снятия лайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{itemId}/dislike")
    @Operation(summary = "Дизлайк")
    public ResponseEntity<StyleFeedItemVoteResponseDto> dislike(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "false") boolean isSwipe) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(styleFeedItemService.dislikeFeedItem(userId, itemId, isSwipe));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка дизлайка style feed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка дизлайка {}: {}", itemId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{itemId}/dislike")
    @Operation(summary = "Снять дизлайк")
    public ResponseEntity<StyleFeedItemVoteResponseDto> undislike(@PathVariable Long itemId) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(styleFeedItemService.undislikeFeedItem(userId, itemId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка снятия дизлайка: {}", e.getMessage(), e);
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
