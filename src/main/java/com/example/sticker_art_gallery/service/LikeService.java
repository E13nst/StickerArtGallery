package com.example.sticker_art_gallery.service;

import com.example.sticker_art_gallery.dto.LikeDto;
import com.example.sticker_art_gallery.dto.LikeResponseDto;
import com.example.sticker_art_gallery.dto.LikeToggleResult;
import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.StickerSetWithLikesDto;
import com.example.sticker_art_gallery.model.Like;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.LikeRepository;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.repository.DislikeRepository;
import com.example.sticker_art_gallery.service.swipe.SwipeTrackingService;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
    private final StickerSetService stickerSetService;
    private final DislikeRepository dislikeRepository;
    private final SwipeTrackingService swipeTrackingService;
    
    public LikeService(LikeRepository likeRepository, 
                      StickerSetRepository stickerSetRepository, 
                      CacheManager cacheManager, 
                      @Lazy StickerSetService stickerSetService,
                      DislikeRepository dislikeRepository,
                      SwipeTrackingService swipeTrackingService) {
        this.likeRepository = likeRepository;
        this.stickerSetRepository = stickerSetRepository;
        this.cacheManager = cacheManager;
        this.stickerSetService = stickerSetService;
        this.dislikeRepository = dislikeRepository;
        this.swipeTrackingService = swipeTrackingService;
    }
    
    /**
     * Поставить лайк стикерсету
     * Если у пользователя уже есть дизлайк, он будет удален
     * 
     * @param userId ID пользователя
     * @param stickerSetId ID стикерсета
     * @param isSwipe флаг, что это свайп (для отслеживания и начисления наград)
     */
    public LikeResponseDto likeStickerSet(Long userId, Long stickerSetId, boolean isSwipe) {
        LOGGER.info("❤️ Пользователь {} лайкает стикерсет {}", userId, stickerSetId);
        
        // Проверяем существование стикерсета
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        // Проверяем, не лайкнул ли уже пользователь
        if (likeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId)) {
            throw new IllegalArgumentException("Вы уже лайкнули этот стикерсет");
        }
        
        // Взаимоисключающая логика: если есть дизлайк, удаляем его
        if (dislikeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId)) {
            LOGGER.info("🔄 Удаление дизлайка перед постановкой лайка для пользователя {} и стикерсета {}", userId, stickerSetId);
            dislikeRepository.deleteByUserIdAndStickerSetId(userId, stickerSetId);
            // Обновляем счетчик дизлайков
            stickerSetRepository.decrementDislikesCount(stickerSetId);
            stickerSetRepository.recalculateDislikesCount(stickerSetId);
        }
        
        // Создаем лайк
        Like like = new Like();
        like.setUserId(userId);
        like.setStickerSet(stickerSet);
        
        Like savedLike = likeRepository.save(like);
        // Денормализованный счётчик - сначала инкремент для быстродействия
        stickerSetRepository.incrementLikesCount(stickerSetId);
        // Пересчет агрегации для гарантии корректности (исправляет расхождения)
        stickerSetRepository.recalculateLikesCount(stickerSetId);
        // Инвалидируем кэши, зависящие от данных стикерсета
        evictStickerSetCaches(stickerSet);
        long totalLikes = getLikesCount(stickerSetId);
        LOGGER.info("✅ Лайк успешно поставлен: {}, всего лайков: {}", savedLike.getId(), totalLikes);
        
        // Если это свайп, записываем его для отслеживания и начисления наград
        if (isSwipe) {
            try {
                swipeTrackingService.recordSwipe(
                    userId,
                    com.example.sticker_art_gallery.model.swipe.UserSwipeEntity.ActionType.LIKE,
                    savedLike,
                    null
                );
                LOGGER.debug("✅ Свайп (лайк) записан: userId={}, stickerSetId={}", userId, stickerSetId);
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка при записи свайпа: {}", e.getMessage(), e);
                // Не прерываем транзакцию - лайк уже поставлен
            }
        }

        LikeResponseDto response = new LikeResponseDto();
        response.setId(savedLike.getId());
        response.setUserId(savedLike.getUserId());
        response.setStickerSetId(stickerSetId);
        response.setCreatedAt(savedLike.getCreatedAt());
        response.setLiked(true);
        response.setTotalLikes(totalLikes);
        response.setSwipe(isSwipe);
        
        return response;
    }
    
    /**
     * Убрать лайк со стикерсета
     */
    public LikeResponseDto unlikeStickerSet(Long userId, Long stickerSetId) {
        LOGGER.info("💔 Пользователь {} убирает лайк со стикерсета {}", userId, stickerSetId);
        
        Like like = likeRepository.findByUserIdAndStickerSetId(userId, stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Лайк не найден"));
        
        // Сохраняем информацию о лайке перед удалением
        Long likeId = like.getId();
        LocalDateTime createdAt = like.getCreatedAt();
        
        likeRepository.delete(like);
        // Денормализованный счётчик - сначала декремент для быстродействия
        stickerSetRepository.decrementLikesCount(stickerSetId);
        // Пересчет агрегации для гарантии корректности (исправляет расхождения)
        stickerSetRepository.recalculateLikesCount(stickerSetId);
        // Инвалидируем кэши, зависящие от данных стикерсета
        stickerSetRepository.findById(stickerSetId).ifPresent(this::evictStickerSetCaches);
        long totalLikes = getLikesCount(stickerSetId);
        LOGGER.info("✅ Лайк успешно удален, всего лайков: {}", totalLikes);
        
        LikeResponseDto response = new LikeResponseDto();
        response.setId(likeId);
        response.setUserId(userId);
        response.setStickerSetId(stickerSetId);
        response.setCreatedAt(createdAt);
        response.setLiked(false);
        response.setTotalLikes(totalLikes);
        
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
     * Переключить лайк (поставить если нет, убрать если есть)
     */
    public LikeToggleResult toggleLike(Long userId, Long stickerSetId) {
        LOGGER.info("🔄 Пользователь {} переключает лайк стикерсета {}", userId, stickerSetId);
        
        boolean exists = likeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId);
        
        if (exists) {
            LikeResponseDto result = unlikeStickerSet(userId, stickerSetId);
            long totalDislikes = dislikeRepository.existsByUserIdAndStickerSetId(userId, stickerSetId) 
                ? getDislikesCountFromStickerSet(stickerSetId) : 0;
            LOGGER.info("✅ Лайк убран, всего лайков: {}", result.getTotalLikes());
            return new LikeToggleResult(result.isLiked(), result.getTotalLikes(), totalDislikes);
        } else {
            LikeResponseDto result = likeStickerSet(userId, stickerSetId, false);
            long totalDislikes = getDislikesCountFromStickerSet(stickerSetId);
            LOGGER.info("✅ Лайк поставлен, всего лайков: {}", result.getTotalLikes());
            return new LikeToggleResult(result.isLiked(), result.getTotalLikes(), totalDislikes);
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
     * Получить количество дизлайков стикерсета (вспомогательный метод)
     */
    @Transactional(readOnly = true)
    public long getDislikesCountFromStickerSet(Long stickerSetId) {
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
    public PageResponse<StickerSetDto> getLikedStickerSets(Long userId, PageRequest pageRequest, String language, boolean shortInfo) {
        return getLikedStickerSets(userId, pageRequest, language, shortInfo, false);
    }
    
    /**
     * Получить лайкнутые стикерсеты пользователя
     * @param preview возвращать только 1 случайный стикер в telegramStickerSetInfo
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetDto> getLikedStickerSets(Long userId, PageRequest pageRequest, String language, boolean shortInfo, boolean preview) {
        LOGGER.debug("📋 Получение лайкнутых стикерсетов пользователя {} с пагинацией: page={}, size={}, shortInfo={}, preview={}", 
                userId, pageRequest.getPage(), pageRequest.getSize(), shortInfo, preview);
        
        Page<StickerSet> likedStickerSets = likeRepository.findLikedStickerSetsByUserId(userId, pageRequest.toPageable());
        
        // Обогащаем данными из Telegram Bot API с учетом shortInfo и preview
        List<StickerSetDto> dtos = stickerSetService.enrichWithBotApiDataAndCategories(
            likedStickerSets.getContent(), language, userId, shortInfo, preview, false);
        
        return PageResponse.of(likedStickerSets, dtos);
    }
    
    /**
     * Получить лайкнутые стикерсеты пользователя по категориям
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetDto> getLikedStickerSetsByCategories(Long userId, String[] categoryKeys, 
                                                                        PageRequest pageRequest, String language, boolean shortInfo) {
        return getLikedStickerSetsByCategories(userId, categoryKeys, pageRequest, language, shortInfo, false);
    }
    
    /**
     * Получить лайкнутые стикерсеты пользователя по категориям
     * @param preview возвращать только 1 случайный стикер в telegramStickerSetInfo
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetDto> getLikedStickerSetsByCategories(Long userId, String[] categoryKeys, 
                                                                        PageRequest pageRequest, String language, boolean shortInfo, boolean preview) {
        LOGGER.debug("📋 Получение лайкнутых стикерсетов пользователя {} по категориям {} с пагинацией: page={}, size={}, shortInfo={}, preview={}", 
                userId, categoryKeys, pageRequest.getPage(), pageRequest.getSize(), shortInfo, preview);
        
        List<String> categoryKeyList = java.util.Arrays.asList(categoryKeys);
        Page<StickerSet> likedStickerSets = likeRepository.findLikedStickerSetsByUserIdAndCategoryKeys(
                userId, categoryKeyList, pageRequest.toPageable());
        
        // Обогащаем данными из Telegram Bot API с учетом shortInfo и preview
        List<StickerSetDto> dtos = stickerSetService.enrichWithBotApiDataAndCategories(
            likedStickerSets.getContent(), language, userId, shortInfo, preview, false);
        
        return PageResponse.of(likedStickerSets, dtos);
    }
    
    /**
     * Поиск лайкнутых стикерсетов пользователя по query (title или description)
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetDto> searchLikedStickerSets(Long userId, String query, 
                                                               Set<String> categoryKeys,
                                                               PageRequest pageRequest, 
                                                               String language, boolean shortInfo) {
        return searchLikedStickerSets(userId, query, categoryKeys, pageRequest, language, shortInfo, false);
    }
    
    /**
     * Поиск лайкнутых стикерсетов пользователя по query (title или description)
     * @param preview возвращать только 1 случайный стикер в telegramStickerSetInfo
     */
    @Transactional(readOnly = true)
    public PageResponse<StickerSetDto> searchLikedStickerSets(Long userId, String query, 
                                                               Set<String> categoryKeys,
                                                               PageRequest pageRequest, 
                                                               String language, boolean shortInfo, boolean preview) {
        LOGGER.debug("🔍 Поиск лайкнутых стикерсетов пользователя {} по query='{}' с пагинацией: page={}, size={}, shortInfo={}, preview={}", 
                userId, query, pageRequest.getPage(), pageRequest.getSize(), shortInfo, preview);
        
        Page<StickerSet> likedStickerSets = likeRepository.searchLikedStickerSets(
                userId, query, categoryKeys, pageRequest.toPageable());
        
        // Обогащаем данными из Telegram Bot API с учетом shortInfo и preview
        List<StickerSetDto> dtos = stickerSetService.enrichWithBotApiDataAndCategories(
            likedStickerSets.getContent(), language, userId, shortInfo, preview, false);
        
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
