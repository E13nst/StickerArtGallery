package com.example.sticker_art_gallery.service;

import com.example.sticker_art_gallery.dto.DislikeDto;
import com.example.sticker_art_gallery.dto.DislikeResponseDto;
import com.example.sticker_art_gallery.dto.DislikeToggleResult;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.model.Dislike;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.DislikeRepository;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с дизлайками стикерсетов
 */
@Service
@Transactional
public class DislikeService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DislikeService.class);
    
    private final DislikeRepository dislikeRepository;
    private final StickerSetRepository stickerSetRepository;
    private final CacheManager cacheManager;
    private final LikeService likeService;
    
    public DislikeService(DislikeRepository dislikeRepository, 
                         StickerSetRepository stickerSetRepository, 
                         CacheManager cacheManager,
                         LikeService likeService) {
        this.dislikeRepository = dislikeRepository;
        this.stickerSetRepository = stickerSetRepository;
        this.cacheManager = cacheManager;
        this.likeService = likeService;
    }
    
    /**
     * Поставить дизлайк стикерсету
     * Если у пользователя уже есть лайк, он будет удален
     */
    public DislikeResponseDto dislikeStickerSet(Long userId, Long stickerSetId) {
        LOGGER.info("👎 Пользователь {} дизлайкает стикерсет {}", userId, stickerSetId);
        
        // Проверяем существование стикерсета
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        // Проверяем, не дизлайкнул ли уже пользователь
        if (dislikeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId)) {
            throw new IllegalArgumentException("Вы уже дизлайкнули этот стикерсет");
        }
        
        // Взаимоисключающая логика: если есть лайк, удаляем его
        if (likeService.isLikedByUser(userId, stickerSetId)) {
            LOGGER.info("🔄 Удаление лайка перед постановкой дизлайка для пользователя {} и стикерсета {}", userId, stickerSetId);
            likeService.unlikeStickerSet(userId, stickerSetId);
        }
        
        // Создаем дизлайк
        Dislike dislike = new Dislike();
        dislike.setUserId(userId);
        dislike.setStickerSet(stickerSet);
        
        Dislike savedDislike = dislikeRepository.save(dislike);
        // Денормализованный счётчик - сначала инкремент для быстродействия
        stickerSetRepository.incrementDislikesCount(stickerSetId);
        // Пересчет агрегации для гарантии корректности (исправляет расхождения)
        stickerSetRepository.recalculateDislikesCount(stickerSetId);
        // Инвалидируем кэши, зависящие от данных стикерсета
        evictStickerSetCaches(stickerSet);
        long totalDislikes = getDislikesCount(stickerSetId);
        LOGGER.info("✅ Дизлайк успешно поставлен: {}, всего дизлайков: {}", savedDislike.getId(), totalDislikes);
        
        DislikeResponseDto response = new DislikeResponseDto();
        response.setId(savedDislike.getId());
        response.setUserId(savedDislike.getUserId());
        response.setStickerSetId(stickerSetId);
        response.setCreatedAt(savedDislike.getCreatedAt());
        response.setDisliked(true);
        response.setTotalDislikes(totalDislikes);
        
        return response;
    }
    
    /**
     * Убрать дизлайк со стикерсета
     */
    public DislikeResponseDto undislikeStickerSet(Long userId, Long stickerSetId) {
        LOGGER.info("💔 Пользователь {} убирает дизлайк со стикерсета {}", userId, stickerSetId);
        
        Dislike dislike = dislikeRepository.findByUserIdAndStickerSetId(userId, stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Дизлайк не найден"));
        
        // Сохраняем информацию о дизлайке перед удалением
        Long dislikeId = dislike.getId();
        LocalDateTime createdAt = dislike.getCreatedAt();
        
        dislikeRepository.delete(dislike);
        // Денормализованный счётчик - сначала декремент для быстродействия
        stickerSetRepository.decrementDislikesCount(stickerSetId);
        // Пересчет агрегации для гарантии корректности (исправляет расхождения)
        stickerSetRepository.recalculateDislikesCount(stickerSetId);
        // Инвалидируем кэши, зависящие от данных стикерсета
        stickerSetRepository.findById(stickerSetId).ifPresent(this::evictStickerSetCaches);
        long totalDislikes = getDislikesCount(stickerSetId);
        LOGGER.info("✅ Дизлайк успешно удален, всего дизлайков: {}", totalDislikes);
        
        DislikeResponseDto response = new DislikeResponseDto();
        response.setId(dislikeId);
        response.setUserId(userId);
        response.setStickerSetId(stickerSetId);
        response.setCreatedAt(createdAt);
        response.setDisliked(false);
        response.setTotalDislikes(totalDislikes);
        
        return response;
    }

    private void evictStickerSetCaches(StickerSet stickerSet) {
        if (stickerSet == null || cacheManager == null) return;
        try {
            Cache cache = cacheManager.getCache("stickerSetInfo");
            if (cache != null && stickerSet.getName() != null) {
                cache.evict(stickerSet.getName());
            }
        } catch (Exception e) {
            LOGGER.debug("⚠️ Не удалось инвалидировать кэш stickerSetInfo для {}: {}", stickerSet.getName(), e.getMessage());
        }
    }
    
    /**
     * Переключить дизлайк (поставить если нет, убрать если есть)
     */
    public DislikeToggleResult toggleDislike(Long userId, Long stickerSetId) {
        LOGGER.info("🔄 Пользователь {} переключает дизлайк стикерсета {}", userId, stickerSetId);
        
        boolean exists = dislikeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId);
        
        if (exists) {
            DislikeResponseDto result = undislikeStickerSet(userId, stickerSetId);
            LOGGER.info("✅ Дизлайк убран, всего дизлайков: {}", result.getTotalDislikes());
            return new DislikeToggleResult(result.isDisliked(), result.getTotalDislikes());
        } else {
            DislikeResponseDto result = dislikeStickerSet(userId, stickerSetId);
            LOGGER.info("✅ Дизлайк поставлен, всего дизлайков: {}", result.getTotalDislikes());
            return new DislikeToggleResult(result.isDisliked(), result.getTotalDislikes());
        }
    }
    
    /**
     * Получить количество дизлайков стикерсета
     */
    @Transactional(readOnly = true)
    public long getDislikesCount(Long stickerSetId) {
        return stickerSetRepository.findById(stickerSetId)
                .map(ss -> ss.getDislikesCount() == null ? 0 : ss.getDislikesCount().longValue())
                .orElse(0L);
    }
    
    /**
     * Проверить существование стикерсета
     */
    @Transactional(readOnly = true)
    public boolean stickerSetExists(Long stickerSetId) {
        return stickerSetRepository.existsById(stickerSetId);
    }
    
    /**
     * Проверить, дизлайкнул ли пользователь стикерсет
     */
    @Transactional(readOnly = true)
    public boolean isDislikedByUser(Long userId, Long stickerSetId) {
        return dislikeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId);
    }
    
    /**
     * Получить все дизлайки пользователя
     */
    @Transactional(readOnly = true)
    public PageResponse<DislikeDto> getUserDislikes(Long userId, PageRequest pageRequest) {
        LOGGER.debug("📋 Получение дизлайков пользователя {} с пагинацией: page={}, size={}", 
                userId, pageRequest.getPage(), pageRequest.getSize());
        
        Page<Dislike> dislikes = dislikeRepository.findByUserId(userId, pageRequest.toPageable());
        
        List<DislikeDto> dtos = dislikes.getContent().stream()
            .map(DislikeDto::fromEntity)
            .collect(Collectors.toList());
        
        return PageResponse.of(dislikes, dtos);
    }
}
