package com.example.sticker_art_gallery.controller.meme;

import com.example.sticker_art_gallery.dto.meme.MemeCandidateDto;
import com.example.sticker_art_gallery.dto.meme.MemeCandidateVoteResponseDto;
import com.example.sticker_art_gallery.service.meme.MemeCandidateService;
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

/**
 * Контроллер ленты оценки мем-кандидатов.
 * Воспроизводит паттерн LikeController / DislikeController для стикерсетов.
 */
@RestController
@RequestMapping("/api/meme-candidates")
@Tag(name = "Meme Candidates", description = "Лента оценки мем-кандидатов (лайки/дизлайки)")
public class MemeCandidateController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemeCandidateController.class);

    private final MemeCandidateService memeCandidateService;

    public MemeCandidateController(MemeCandidateService memeCandidateService) {
        this.memeCandidateService = memeCandidateService;
    }

    @GetMapping("/feed/next")
    @Operation(
            summary = "Получить следующий мем-кандидат из ленты",
            description = "Возвращает случайный VISIBLE кандидат, который текущий пользователь ещё не оценивал. " +
                          "Перед выдачей проверяется дневной лимит свайпов.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Кандидат найден"),
            @ApiResponse(responseCode = "204", description = "Нет доступных кандидатов"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "429", description = "Достигнут дневной лимит свайпов")
    })
    public ResponseEntity<MemeCandidateDto> getNext() {
        try {
            Long userId = getCurrentUserId();
            return memeCandidateService.getNextForFeed(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (com.example.sticker_art_gallery.exception.SwipeLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при получении мем-кандидата: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{candidateId}/like")
    @Operation(summary = "Лайкнуть мем-кандидата")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Лайк поставлен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<MemeCandidateVoteResponseDto> like(
            @PathVariable Long candidateId,
            @Parameter(description = "Флаг свайпа (для учёта в дневном лимите и начисления наград)", example = "false")
            @RequestParam(defaultValue = "false") boolean isSwipe) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(memeCandidateService.likeCandidate(userId, candidateId, isSwipe));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка лайка мем-кандидата: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при лайке мем-кандидата {}: {}", candidateId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{candidateId}/like")
    @Operation(summary = "Убрать лайк с мем-кандидата")
    public ResponseEntity<MemeCandidateVoteResponseDto> unlike(@PathVariable Long candidateId) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(memeCandidateService.unlikeCandidate(userId, candidateId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при снятии лайка: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{candidateId}/dislike")
    @Operation(summary = "Дизлайкнуть мем-кандидата")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Дизлайк поставлен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<MemeCandidateVoteResponseDto> dislike(
            @PathVariable Long candidateId,
            @Parameter(description = "Флаг свайпа (для учёта в дневном лимите и начисления наград)", example = "false")
            @RequestParam(defaultValue = "false") boolean isSwipe) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(memeCandidateService.dislikeCandidate(userId, candidateId, isSwipe));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ошибка дизлайка мем-кандидата: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при дизлайке мем-кандидата {}: {}", candidateId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{candidateId}/dislike")
    @Operation(summary = "Убрать дизлайк с мем-кандидата")
    public ResponseEntity<MemeCandidateVoteResponseDto> undislike(@PathVariable Long candidateId) {
        try {
            Long userId = getCurrentUserId();
            return ResponseEntity.ok(memeCandidateService.undislikeCandidate(userId, candidateId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Ошибка при снятии дизлайка: {}", e.getMessage(), e);
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
