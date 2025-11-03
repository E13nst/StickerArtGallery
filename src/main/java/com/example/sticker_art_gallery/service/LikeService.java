package com.example.sticker_art_gallery.service;

import com.example.sticker_art_gallery.dto.LikeDto;
import com.example.sticker_art_gallery.dto.LikeToggleResult;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.StickerSetWithLikesDto;
import com.example.sticker_art_gallery.model.Like;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.LikeRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с лайками стикерсетов
 */
@Service
@Transactional
public class LikeService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LikeService.class);
    
    private final LikeRepository likeRepository;
    private final StickerSetRepository stickerSetRepository;
    private final CacheManager cacheManager;
    
    public LikeService(LikeRepository likeRepository, StickerSetRepository stickerSetRepository, CacheManager cacheManager) {
        this.likeRepository = likeRepository;
        this.stickerSetRepository = stickerSetRepository;
        this.cacheManager = cacheManager;
    }
    
    /**
     * Поставить лайк стикерсету
     */
    public LikeDto likeStickerSet(Long userId, Long stickerSetId) {
        LOGGER.info("❤️ Пользователь {} лайкает стикерсет {}", userId, stickerSetId);
        
        // Проверяем существование стикерсета
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        // Проверяем, не лайкнул ли уже пользователь
        if (likeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId)) {
            throw new IllegalArgumentException("Вы уже лайкнули этот стикерсет");
        }
        
        // Создаем лайк
        Like like = new Like();
        like.setUserId(userId);
        like.setStickerSet(stickerSet);
        
        Like savedLike = likeRepository.save(like);
        // Денормализованный счётчик
        stickerSetRepository.incrementLikesCount(stickerSetId);
        // Инвалидируем кэши, зависящие от данных стикерсета
        evictStickerSetCaches(stickerSet);
        LOGGER.info("✅ Лайк успешно поставлен: {}", savedLike.getId());
        
        return LikeDto.fromEntity(savedLike);
    }
    
    /**
     * Убрать лайк со стикерсета
     */
    public void unlikeStickerSet(Long userId, Long stickerSetId) {
        LOGGER.info("💔 Пользователь {} убирает лайк со стикерсета {}", userId, stickerSetId);
        
        Like like = likeRepository.findByUserIdAndStickerSetId(userId, stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Лайк не найден"));
        
        likeRepository.delete(like);
        // Денормализованный счётчик
        stickerSetRepository.decrementLikesCount(stickerSetId);
        // Инвалидируем кэши, зависящие от данных стикерсета
        stickerSetRepository.findById(stickerSetId).ifPresent(this::evictStickerSetCaches);
        LOGGER.info("✅ Лайк успешно удален");
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
     * Переключить лайк (поставить если нет, убрать если есть)
     */
    public LikeToggleResult toggleLike(Long userId, Long stickerSetId) {
        LOGGER.info("🔄 Пользователь {} переключает лайк стикерсета {}", userId, stickerSetId);
        
        boolean exists = likeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId);
        
        if (exists) {
            unlikeStickerSet(userId, stickerSetId);
            long totalLikes = getLikesCount(stickerSetId);
            LOGGER.info("✅ Лайк убран, всего лайков: {}", totalLikes);
            return new LikeToggleResult(false, totalLikes);
        } else {
            likeStickerSet(userId, stickerSetId);
            long totalLikes = getLikesCount(stickerSetId);
            LOGGER.info("✅ Лайк поставлен, всего лайков: {}", totalLikes);
            return new LikeToggleResult(true, totalLikes);
        }
    }
    
    /**
     * Получить количество лайков стикерсета
     */
    @Transactional(readOnly = true)
    public long getLikesCount(Long stickerSetId) {
        return stickerSetRepository.findById(stickerSetId)
                .map(ss -> ss.getLikesCount() == null ? 0 : ss.getLikesCount().longValue())
                .orElse(0L);
    }
    
    /**
     * Проверить, лайкнул ли пользователь стикерсет
     */
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long userId, Long stickerSetId) {
        return likeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId);
    }
    
    /**
     * Получить лайкнутые стикерсеты пользователя
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetDto> getLikedStickerSets(Long userId, PageRequest pageRequest, String language) {
        LOGGER.debug("📋 Получение лайкнутых стикерсетов пользователя {} с пагинацией: page={}, size={}", 
                userId, pageRequest.getPage(), pageRequest.getSize());
        
        Page<StickerSet> likedStickerSets = likeRepository.findLikedStickerSetsByUserId(userId, pageRequest.toPageable());
        
        List<StickerSetDto> dtos = likedStickerSets.getContent().stream()
            .map(stickerSet -> StickerSetDto.fromEntity(stickerSet, language))
            .collect(Collectors.toList());
        
        return PageResponse.of(likedStickerSets, dtos);
    }
    
    /**
     * Получить лайкнутые стикерсеты пользователя по категориям
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetDto> getLikedStickerSetsByCategories(Long userId, String[] categoryKeys, 
                                                                        PageRequest pageRequest, String language) {
        LOGGER.debug("📋 Получение лайкнутых стикерсетов пользователя {} по категориям {} с пагинацией: page={}, size={}", 
                userId, categoryKeys, pageRequest.getPage(), pageRequest.getSize());
        
        List<String> categoryKeyList = java.util.Arrays.asList(categoryKeys);
        Page<StickerSet> likedStickerSets = likeRepository.findLikedStickerSetsByUserIdAndCategoryKeys(
                userId, categoryKeyList, pageRequest.toPageable());
        
        List<StickerSetDto> dtos = likedStickerSets.getContent().stream()
            .map(stickerSet -> StickerSetDto.fromEntity(stickerSet, language))
            .collect(Collectors.toList());
        
        return PageResponse.of(likedStickerSets, dtos);
    }
    
    /**
     * Получить топ стикерсетов по лайкам
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetWithLikesDto> getTopStickerSetsByLikes(PageRequest pageRequest, String language, Long currentUserId) {
        LOGGER.debug("🏆 Получение топ стикерсетов по лайкам с пагинацией: page={}, size={}", 
                pageRequest.getPage(), pageRequest.getSize());
        
        Page<Object[]> results = likeRepository.findTopStickerSetsByLikes(pageRequest.toPageable());
        
        List<StickerSetWithLikesDto> dtos = results.getContent().stream()
            .map(result -> {
                StickerSet stickerSet = (StickerSet) result[0];
                Long likesCount = (Long) result[1];
                
                StickerSetWithLikesDto dto = new StickerSetWithLikesDto();
                dto.setStickerSet(StickerSetDto.fromEntity(stickerSet, language));
                dto.setLikesCount(likesCount);
                
                // Если пользователь авторизован, проверяем, лайкнул ли он этот стикерсет
                if (currentUserId != null) {
                    dto.setLikedByCurrentUser(isLikedByUser(currentUserId, stickerSet.getId()));
                } else {
                    dto.setLikedByCurrentUser(false);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        return PageResponse.of(results, dtos);
    }
    
    /**
     * Получить топ стикерсетов по лайкам c опциональной фильтрацией по официальным
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetWithLikesDto> getTopStickerSetsByLikes(PageRequest pageRequest, String language, Long currentUserId, boolean officialOnly) {
        LOGGER.debug("🏆 Получение {} топ стикерсетов по лайкам с пагинацией: page={}, size={}",
                officialOnly ? "официальных" : "публичных", pageRequest.getPage(), pageRequest.getSize());
        Page<Object[]> results = officialOnly
                ? likeRepository.findTopOfficialStickerSetsByLikes(pageRequest.toPageable())
                : likeRepository.findTopStickerSetsByLikes(pageRequest.toPageable());
        
        List<StickerSetWithLikesDto> dtos = results.getContent().stream()
            .map(result -> {
                StickerSet stickerSet = (StickerSet) result[0];
                Long likesCount = (Long) result[1];
                
                StickerSetWithLikesDto dto = new StickerSetWithLikesDto();
                dto.setStickerSet(StickerSetDto.fromEntity(stickerSet, language));
                dto.setLikesCount(likesCount);
                
                if (currentUserId != null) {
                    dto.setLikedByCurrentUser(isLikedByUser(currentUserId, stickerSet.getId()));
                } else {
                    dto.setLikedByCurrentUser(false);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        return PageResponse.of(results, dtos);
    }

    /**
     * Получить топ стикерсетов по лайкам c фильтрами officialOnly/authorId/hasAuthorOnly
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetWithLikesDto> getTopStickerSetsByLikes(PageRequest pageRequest, String language, Long currentUserId,
                                                                         boolean officialOnly, Long authorId, boolean hasAuthorOnly) {
        LOGGER.debug("🏆 Получение {} топ стикерсетов по лайкам{}: page={}, size={}",
                officialOnly ? "официальных" : "публичных",
                authorId != null ? (" автора=" + authorId) : (hasAuthorOnly ? " (только с автором)" : ""),
                pageRequest.getPage(), pageRequest.getSize());

        Page<Object[]> results = likeRepository.findTopStickerSetsByLikesFiltered(officialOnly, authorId, hasAuthorOnly, pageRequest.toPageable());

        List<StickerSetWithLikesDto> dtos = results.getContent().stream()
            .map(result -> {
                StickerSet stickerSet = (StickerSet) result[0];
                Long likesCount = (Long) result[1];

                StickerSetWithLikesDto dto = new StickerSetWithLikesDto();
                dto.setStickerSet(StickerSetDto.fromEntity(stickerSet, language));
                dto.setLikesCount(likesCount);

                if (currentUserId != null) {
                    dto.setLikedByCurrentUser(isLikedByUser(currentUserId, stickerSet.getId()));
                } else {
                    dto.setLikedByCurrentUser(false);
                }

                return dto;
            })
            .collect(Collectors.toList());

        return PageResponse.of(results, dtos);
    }
    
    /**
     * Получить список ID стикерсетов, которые лайкнул пользователь
     */
    @Transactional(readOnly = true)
    public List<Long> getLikedStickerSetIds(Long userId, List<Long> stickerSetIds) {
        return likeRepository.findLikedStickerSetIdsByUserId(userId, stickerSetIds);
    }
    
    /**
     * Получить все лайки пользователя
     */
    @Transactional(readOnly = true)
    public PageResponse<LikeDto> getUserLikes(Long userId, PageRequest pageRequest) {
        LOGGER.debug("📋 Получение лайков пользователя {} с пагинацией: page={}, size={}", 
                userId, pageRequest.getPage(), pageRequest.getSize());
        
        Page<Like> likes = likeRepository.findByUserId(userId, pageRequest.toPageable());
        
        List<LikeDto> dtos = likes.getContent().stream()
            .map(LikeDto::fromEntity)
            .collect(Collectors.toList());
        
        return PageResponse.of(likes, dtos);
    }
}
