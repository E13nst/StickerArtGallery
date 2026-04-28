package com.example.sticker_art_gallery.service.stylefeed;

import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedItemDto;
import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedItemVoteResponseDto;
import com.example.sticker_art_gallery.model.stylefeed.CandidateFeedVisibility;
import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemDislikeEntity;
import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemEntity;
import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemLikeEntity;
import com.example.sticker_art_gallery.model.swipe.UserSwipeEntity;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemDislikeRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemLikeRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemRepository;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.example.sticker_art_gallery.service.swipe.SwipeTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис ленты style feed (оценка пресетов пользователями).
 */
@Service
@Transactional
public class StyleFeedItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFeedItemService.class);

    private final StyleFeedItemRepository styleFeedItemRepository;
    private final StyleFeedItemLikeRepository likeRepository;
    private final StyleFeedItemDislikeRepository dislikeRepository;
    private final SwipeTrackingService swipeTrackingService;
    private final ImageStorageService imageStorageService;

    public StyleFeedItemService(StyleFeedItemRepository styleFeedItemRepository,
                               StyleFeedItemLikeRepository likeRepository,
                               StyleFeedItemDislikeRepository dislikeRepository,
                               SwipeTrackingService swipeTrackingService,
                               ImageStorageService imageStorageService) {
        this.styleFeedItemRepository = styleFeedItemRepository;
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.swipeTrackingService = swipeTrackingService;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public List<StyleFeedItemDto> listForAdmin(Optional<CandidateFeedVisibility> visibility) {
        List<StyleFeedItemEntity> entities = visibility
                .map(styleFeedItemRepository::findByVisibilityOrderByCreatedAtDesc)
                .orElseGet(styleFeedItemRepository::findAllByOrderByCreatedAtDesc);
        return entities.stream().map(StyleFeedItemDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Optional<StyleFeedItemDto> getNextForFeed(Long userId) {
        swipeTrackingService.checkDailyLimit(userId);
        return styleFeedItemRepository.findRandomNotRatedByUser(userId)
                .map(StyleFeedItemDto::fromEntity);
    }

    public StyleFeedItemVoteResponseDto likeFeedItem(Long userId, Long itemId, boolean isSwipe) {
        LOGGER.info("Пользователь {} лайкает style feed item {} (isSwipe={})", userId, itemId, isSwipe);

        StyleFeedItemEntity item = styleFeedItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена: " + itemId));

        if (likeRepository.existsByUserIdAndStyleFeedItem_Id(userId, itemId)) {
            throw new IllegalArgumentException("Вы уже лайкнули эту запись");
        }

        dislikeRepository.findByUserIdAndItemIdForUpdate(userId, itemId)
                .ifPresent(existing -> {
                    LOGGER.info("Удаление дизлайка перед лайком: userId={}, itemId={}", userId, itemId);
                    dislikeRepository.delete(existing);
                    styleFeedItemRepository.decrementDislikesCount(itemId);
                });

        StyleFeedItemLikeEntity like = new StyleFeedItemLikeEntity();
        like.setUserId(userId);
        like.setStyleFeedItem(item);
        StyleFeedItemLikeEntity savedLike = likeRepository.save(like);

        styleFeedItemRepository.applyLike(itemId);

        if (isSwipe) {
            try {
                swipeTrackingService.recordStyleFeedSwipe(
                        userId,
                        UserSwipeEntity.ActionType.LIKE,
                        savedLike,
                        null);
            } catch (Exception e) {
                LOGGER.error("Ошибка при записи свайпа (лайк style feed): {}", e.getMessage(), e);
            }
        }

        StyleFeedItemEntity refreshed = styleFeedItemRepository.findById(itemId).orElse(item);
        return buildResponse(savedLike.getId(), userId, itemId, true, false,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), isSwipe);
    }

    public StyleFeedItemVoteResponseDto unlikeFeedItem(Long userId, Long itemId) {
        StyleFeedItemLikeEntity like = likeRepository
                .findByUserIdAndStyleFeedItem_Id(userId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Лайк не найден"));

        likeRepository.delete(like);
        styleFeedItemRepository.decrementLikesCount(itemId);

        StyleFeedItemEntity refreshed = styleFeedItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена: " + itemId));
        return buildResponse(like.getId(), userId, itemId, false, false,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), false);
    }

    public StyleFeedItemVoteResponseDto dislikeFeedItem(Long userId, Long itemId, boolean isSwipe) {
        LOGGER.info("Пользователь {} дизлайкает style feed item {} (isSwipe={})", userId, itemId, isSwipe);

        StyleFeedItemEntity item = styleFeedItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена: " + itemId));

        if (dislikeRepository.existsByUserIdAndStyleFeedItem_Id(userId, itemId)) {
            throw new IllegalArgumentException("Вы уже дизлайкнули эту запись");
        }

        likeRepository.findByUserIdAndItemIdForUpdate(userId, itemId)
                .ifPresent(existing -> {
                    LOGGER.info("Удаление лайка перед дизлайком: userId={}, itemId={}", userId, itemId);
                    likeRepository.delete(existing);
                    styleFeedItemRepository.decrementLikesCount(itemId);
                });

        StyleFeedItemDislikeEntity dislike = new StyleFeedItemDislikeEntity();
        dislike.setUserId(userId);
        dislike.setStyleFeedItem(item);
        StyleFeedItemDislikeEntity savedDislike = dislikeRepository.save(dislike);

        styleFeedItemRepository.applyDislikeAndAutoHide(itemId);

        if (isSwipe) {
            try {
                swipeTrackingService.recordStyleFeedSwipe(
                        userId,
                        UserSwipeEntity.ActionType.DISLIKE,
                        null,
                        savedDislike);
            } catch (Exception e) {
                LOGGER.error("Ошибка при записи свайпа (дизлайк style feed): {}", e.getMessage(), e);
            }
        }

        StyleFeedItemEntity refreshed = styleFeedItemRepository.findById(itemId).orElseThrow();
        return buildResponse(savedDislike.getId(), userId, itemId, false, true,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), isSwipe);
    }

    public StyleFeedItemVoteResponseDto undislikeFeedItem(Long userId, Long itemId) {
        StyleFeedItemDislikeEntity dislike = dislikeRepository
                .findByUserIdAndStyleFeedItem_Id(userId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Дизлайк не найден"));

        dislikeRepository.delete(dislike);
        styleFeedItemRepository.decrementDislikesCount(itemId);

        StyleFeedItemEntity refreshed = styleFeedItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена: " + itemId));
        return buildResponse(dislike.getId(), userId, itemId, false, false,
                refreshed.getLikesCount(), refreshed.getDislikesCount(), false);
    }

    public StyleFeedItemDto setAdminVisibilityOverride(Long itemId, String action) {
        if (!styleFeedItemRepository.existsById(itemId)) {
            throw new IllegalArgumentException("Запись не найдена: " + itemId);
        }
        Boolean override = switch (action.toUpperCase()) {
            case "SHOW"  -> true;
            case "HIDE"  -> false;
            case "RESET" -> null;
            default -> throw new IllegalArgumentException("Неверное действие: " + action + ". Допустимо: SHOW, HIDE, RESET");
        };
        styleFeedItemRepository.setAdminVisibilityOverride(itemId, override);
        return styleFeedItemRepository.findById(itemId)
                .map(StyleFeedItemDto::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена после обновления: " + itemId));
    }

    public int hideByStylePresetId(Long stylePresetId) {
        return styleFeedItemRepository.hideByStylePresetId(stylePresetId);
    }

    public int republishByStylePresetId(Long stylePresetId) {
        return styleFeedItemRepository.republishByStylePresetId(stylePresetId);
    }

    public StyleFeedItemDto replacePreview(Long itemId, MultipartFile file) {
        StyleFeedItemEntity item = styleFeedItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена: " + itemId));
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (!contentType.equals("image/png") && !contentType.equals("image/webp") && !contentType.equals("image/jpeg")) {
            throw new IllegalArgumentException("Поддерживаемые форматы: image/png, image/webp, image/jpeg");
        }
        if (file.getSize() > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("Файл слишком большой (максимум 3MB)");
        }

        try {
            byte[] bytes = file.getBytes();
            Long sourceId = item.getStylePreset() != null ? item.getStylePreset().getId() : itemId;
            var stored = imageStorageService.storeStylePresetPreview(sourceId, bytes, contentType);
            item.setCachedImage(stored);
            item.setPreviewOverriddenByAdmin(true);
            item.setPreviewOverriddenAt(OffsetDateTime.now());
            StyleFeedItemEntity saved = styleFeedItemRepository.save(item);
            return StyleFeedItemDto.fromEntity(saved);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось заменить preview: " + e.getMessage(), e);
        }
    }

    private StyleFeedItemVoteResponseDto buildResponse(Long voteId, Long userId, Long itemId,
                                                        boolean liked, boolean disliked,
                                                        int totalLikes, int totalDislikes,
                                                        boolean isSwipe) {
        StyleFeedItemVoteResponseDto resp = new StyleFeedItemVoteResponseDto();
        resp.setId(voteId);
        resp.setUserId(userId);
        resp.setStyleFeedItemId(itemId);
        resp.setLiked(liked);
        resp.setDisliked(disliked);
        resp.setTotalLikes(totalLikes != 0 ? totalLikes : 0);
        resp.setTotalDislikes(totalDislikes != 0 ? totalDislikes : 0);
        resp.setSwipe(isSwipe);
        return resp;
    }
}
