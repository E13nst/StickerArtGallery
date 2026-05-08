package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.deck.*;
import com.example.sticker_art_gallery.exception.SwipeLimitExceededException;
import com.example.sticker_art_gallery.service.deck.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deck")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Deck", description = "Персональная swipe-колода мини-приложения")
@SecurityRequirement(name = "TelegramInitData")
public class DeckController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckController.class);

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping("/cards")
    @Operation(summary = "Текущая пачка карточек колоды",
            description = "До 20 карточек за запрос (параметр limit), порядок и лимиты определяет сервер.")
    public ResponseEntity<DeckCardsResponse> getCards(
            @RequestParam(name = "limit", required = false) Integer limit) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(deckService.getDeckCards(userId, limit));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            LOGGER.error("deck/cards: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/action")
    @Operation(summary = "Действие по карточке колоды (лайк/дизлайк стиля, claim бонусов, dismiss)")
    public ResponseEntity<DeckCardActionResponse> performAction(@Valid @RequestBody DeckCardActionRequest request) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(deckService.performAction(userId, request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (SwipeLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("deck/action: {}", e.getMessage());
            DeckCardActionResponse err = new DeckCardActionResponse();
            err.setSuccess(false);
            err.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            LOGGER.error("deck/action: {}", e.getMessage(), e);
            DeckCardActionResponse err = new DeckCardActionResponse();
            err.setSuccess(false);
            err.setMessage("internal_error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
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
