package com.example.sticker_art_gallery.service.meme;

import com.example.sticker_art_gallery.dto.meme.MemeCandidateDto;
import com.example.sticker_art_gallery.dto.meme.MemeCandidateVoteResponseDto;
import com.example.sticker_art_gallery.model.meme.MemeCandidateDislikeEntity;
import com.example.sticker_art_gallery.model.meme.MemeCandidateEntity;
import com.example.sticker_art_gallery.model.meme.MemeCandidateLikeEntity;
import com.example.sticker_art_gallery.model.swipe.UserSwipeEntity;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateDislikeRepository;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateLikeRepository;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateRepository;
import com.example.sticker_art_gallery.service.swipe.SwipeTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Сервис для работы с лентой мем-кандидатов.
 * Воспроизводит паттерн LikeService / DislikeService для стикерсетов.
 */
@Service
@Transactional
public class MemeCandidateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemeCandidateService.class);

    private final MemeCandidateRepository candidateRepository;
    private final MemeCandidateLikeRepository likeRepository;
    private final MemeCandidateDislikeRepository dislikeRepository;
    private final SwipeTrackingService swipeTrackingService;

    public MemeCandidateService(MemeCandidateRepository candidateRepository,
                                MemeCandidateLikeRepository likeRepository,
                                MemeCandidateDislikeRepository dislikeRepository,
                                SwipeTrackingService swipeTrackingService) {
        this.candidateRepository = candidateRepository;
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.swipeTrackingService = swipeTrackingService;
    }

    // =========================================================================
    // Лента
    // =========================================================================

    /**
     * Получить случайный мем-кандидат из ленты, который пользователь ещё не оценил.
     * Перед выдачей проверяется дневной лимит свайпов.
     */
    @Transactional(readOnly = true)
    public Optional<MemeCandidateDto> getNextForFeed(Long userId) {
        swipeTrackingService.checkDailyLimit(userId);
        return candidateRepository.findRandomNotRatedByUser(userId)
                .map(MemeCandidateDto::fromEntity);
    }

    // =========================================================================
    // Лайк
    // =========================================================================

    /**
     * Поставить лайк мем-кандидату.
     * Взаимоисключение с дизлайком выполняется через SELECT FOR UPDATE.
     *
     * @param isSwipe true — свайп (записывается в user_swipes, начисляются награды)
     */
    public MemeCandidateVoteResponseDto likeCandidate(Long userId, Long candidateId, boolean isSwipe) {
        LOGGER.info("Пользователь {} лайкает мем-кандидат {} (isSwipe={})", userId, candidateId, isSwipe);

        MemeCandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Мем-кандидат не найден: " + candidateId));

        if (likeRepository.existsByUserIdAndMemeCandidateId(userId, candidateId)) {
            throw new IllegalArgumentException("Вы уже лайкнули этого кандидата");
        }

        // Взаимоисключение: если есть дизлайк — удаляем его с FOR UPDATE
        dislikeRepository.findByUserIdAndCandidateIdForUpdate(userId, candidateId)
                .ifPresent(existing -> {
                    LOGGER.info("Удаление дизлайка перед лайком: userId={}, candidateId={}", userId, candidateId);
                    dislikeRepository.delete(existing);
                    candidateRepository.decrementDislikesCount(candidateId);
                });

        MemeCandidateLikeEntity like = new MemeCandidateLikeEntity();
        like.setUserId(userId);
        like.setMemeCandidate(candidate);
        MemeCandidateLikeEntity savedLike = likeRepository.save(like);

        candidateRepository.applyLike(candidateId);

        if (isSwipe) {
            try {
                swipeTrackingService.recordMemeSwipe(
                        userId,
                        UserSwipeEntity.ActionType.LIKE,
                        savedLike,
                        null);
            } catch (Exception e) {
                LOGGER.error("Ошибка при записи мем-свайпа (лайк): {}", e.getMessage(), e);
            }
        }

        MemeCandidateEntity refreshed = candidateRepository.findById(candidateId).orElse(candidate);
        return buildResponse(savedLike.getId(), userId, candidateId, true, false,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), isSwipe);
    }

    /**
     * Убрать лайк с мем-кандидата.
     */
    public MemeCandidateVoteResponseDto unlikeCandidate(Long userId, Long candidateId) {
        MemeCandidateLikeEntity like = likeRepository
                .findByUserIdAndMemeCandidateId(userId, candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Лайк не найден"));

        likeRepository.delete(like);
        candidateRepository.decrementLikesCount(candidateId);

        MemeCandidateEntity refreshed = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Мем-кандидат не найден: " + candidateId));
        return buildResponse(like.getId(), userId, candidateId, false, false,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), false);
    }

    // =========================================================================
    // Дизлайк
    // =========================================================================

    /**
     * Поставить дизлайк мем-кандидату.
     * После сохранения выполняется атомарный CAS-апдейт автоскрытия на уровне БД.
     *
     * @param isSwipe true — свайп (записывается в user_swipes, начисляются награды)
     */
    public MemeCandidateVoteResponseDto dislikeCandidate(Long userId, Long candidateId, boolean isSwipe) {
        LOGGER.info("Пользователь {} дизлайкает мем-кандидат {} (isSwipe={})", userId, candidateId, isSwipe);

        MemeCandidateEntity candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Мем-кандидат не найден: " + candidateId));

        if (dislikeRepository.existsByUserIdAndMemeCandidateId(userId, candidateId)) {
            throw new IllegalArgumentException("Вы уже дизлайкнули этого кандидата");
        }

        // Взаимоисключение: если есть лайк — удаляем его с FOR UPDATE
        likeRepository.findByUserIdAndCandidateIdForUpdate(userId, candidateId)
                .ifPresent(existing -> {
                    LOGGER.info("Удаление лайка перед дизлайком: userId={}, candidateId={}", userId, candidateId);
                    likeRepository.delete(existing);
                    candidateRepository.decrementLikesCount(candidateId);
                });

        MemeCandidateDislikeEntity dislike = new MemeCandidateDislikeEntity();
        dislike.setUserId(userId);
        dislike.setMemeCandidate(candidate);
        MemeCandidateDislikeEntity savedDislike = dislikeRepository.save(dislike);

        // Атомарный CAS на уровне БД — автоскрытие при нужных порогах
        candidateRepository.applyDislikeAndAutoHide(candidateId);

        if (isSwipe) {
            try {
                swipeTrackingService.recordMemeSwipe(
                        userId,
                        UserSwipeEntity.ActionType.DISLIKE,
                        null,
                        savedDislike);
            } catch (Exception e) {
                LOGGER.error("Ошибка при записи мем-свайпа (дизлайк): {}", e.getMessage(), e);
            }
        }

        MemeCandidateEntity refreshed = candidateRepository.findById(candidateId).orElse(candidate);
        return buildResponse(savedDislike.getId(), userId, candidateId, false, true,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), isSwipe);
    }

    /**
     * Убрать дизлайк с мем-кандидата.
     */
    public MemeCandidateVoteResponseDto undislikeCandidate(Long userId, Long candidateId) {
        MemeCandidateDislikeEntity dislike = dislikeRepository
                .findByUserIdAndMemeCandidateId(userId, candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Дизлайк не найден"));

        dislikeRepository.delete(dislike);
        candidateRepository.decrementDislikesCount(candidateId);

        MemeCandidateEntity refreshed = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Мем-кандидат не найден: " + candidateId));
        return buildResponse(dislike.getId(), userId, candidateId, false, false,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), false);
    }

    // =========================================================================
    // Admin
    // =========================================================================

    /**
     * Установить admin-override видимости.
     * "SHOW" → override=true, "HIDE" → override=false, "RESET" → override=null.
     */
    public MemeCandidateDto setAdminVisibilityOverride(Long candidateId, String action) {
        if (!candidateRepository.existsById(candidateId)) {
            throw new IllegalArgumentException("Мем-кандидат не найден: " + candidateId);
        }
        Boolean override = switch (action.toUpperCase()) {
            case "SHOW"  -> true;
            case "HIDE"  -> false;
            case "RESET" -> null;
            default -> throw new IllegalArgumentException("Неверное действие: " + action + ". Допустимо: SHOW, HIDE, RESET");
        };
        candidateRepository.setAdminVisibilityOverride(candidateId, override);
        return candidateRepository.findById(candidateId)
                .map(MemeCandidateDto::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Мем-кандидат не найден после обновления: " + candidateId));
    }

    // =========================================================================
    // Вспомогательные методы
    // =========================================================================

    private MemeCandidateVoteResponseDto buildResponse(Long voteId, Long userId, Long candidateId,
                                                        boolean liked, boolean disliked,
                                                        int totalLikes, int totalDislikes,
                                                        boolean isSwipe) {
        MemeCandidateVoteResponseDto resp = new MemeCandidateVoteResponseDto();
        resp.setId(voteId);
        resp.setUserId(userId);
        resp.setMemeCandidateId(candidateId);
        resp.setLiked(liked);
        resp.setDisliked(disliked);
        resp.setTotalLikes(totalLikes != 0 ? totalLikes : 0);
        resp.setTotalDislikes(totalDislikes != 0 ? totalDislikes : 0);
        resp.setSwipe(isSwipe);
        return resp;
    }
}
