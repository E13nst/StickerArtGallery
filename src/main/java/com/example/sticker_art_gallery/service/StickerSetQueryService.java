package com.example.sticker_art_gallery.service;

import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.StickerSetFilterRequest;
import com.example.sticker_art_gallery.exception.UnauthorizedException;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Сервис для обработки запросов к стикерсетам с различными фильтрами.
 * Предоставляет единую точку входа для всех операций поиска и фильтрации.
 */
@Service
public class StickerSetQueryService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetQueryService.class);
    
    private final StickerSetService stickerSetService;
    private final LikeService likeService;
    
    public StickerSetQueryService(StickerSetService stickerSetService, LikeService likeService) {
        this.stickerSetService = stickerSetService;
        this.likeService = likeService;
    }
    
    /**
     * Единая точка входа для получения стикерсетов с любыми фильтрами.
     * Автоматически выбирает правильную стратегию на основе параметров фильтра.
     * 
     * @param filter параметры фильтрации и пагинации
     * @return страница стикерсетов с примененными фильтрами
     * @throws UnauthorizedException если требуется авторизация, но пользователь не авторизован
     */
    public PageResponse<StickerSetDto> findStickerSets(StickerSetFilterRequest filter) {
        LOGGER.debug("🔍 Поиск стикерсетов с фильтром: {}", filter);
        
        // Валидация: проверяем требование авторизации
        if (filter.requiresAuthentication() && filter.getCurrentUserId() == null) {
            LOGGER.warn("⚠️ Попытка запроса лайкнутых стикерсетов без авторизации");
            throw new UnauthorizedException("Authentication required for likedOnly filter");
        }
        
        // Выбор стратегии на основе фильтров
        PageResponse<StickerSetDto> result;
        if (filter.isLikedOnly()) {
            result = findLikedStickerSets(filter);
        } else {
            result = findPublicStickerSets(filter);
        }
        
        LOGGER.debug("✅ Найдено {} стикерсетов (страница {} из {})", 
            result.getContent().size(), result.getPage() + 1, result.getTotalPages());
        
        return result;
    }
    
    /**
     * Получить лайкнутые пользователем стикерсеты
     */
    private PageResponse<StickerSetDto> findLikedStickerSets(StickerSetFilterRequest filter) {
        LOGGER.debug("❤️ Поиск лайкнутых стикерсетов пользователя {} с фильтрами: {}", 
                filter.getCurrentUserId(), filter);
        
        // Используем новый метод с полной поддержкой всех фильтров
        return likeService.getLikedStickerSetsFiltered(
                filter.getCurrentUserId(),
                filter.getCategoryKeys(),
                filter.getType(),
                filter.getUserId(),
                filter.getIsVerified(),
                filter.getPageRequest(),
                filter.getLanguage(),
                filter.isShortInfo(),
                filter.isPreview()
        );
    }
    
    /**
     * Получить публичные стикерсеты с применением фильтров
     */
    private PageResponse<StickerSetDto> findPublicStickerSets(StickerSetFilterRequest filter) {
        LOGGER.debug("🌐 Поиск публичных стикерсетов");
        
        if (filter.hasCategoryFilter()) {
            // Фильтрация по категориям
            LOGGER.debug("🏷️ Применяется фильтр по категориям: {}", filter.getCategoryKeys());
            return stickerSetService.findByCategoryKeys(
                filter.getCategoryKeys().toArray(new String[0]),
                filter.getPageRequest(),
                filter.getLanguage(),
                filter.getCurrentUserId(),
                filter.getType(),
                filter.getUserId(),
                filter.getIsVerified(),
                filter.isShortInfo(),
                filter.isPreview()
            );
        } else {
            // Без фильтрации по категориям
            return stickerSetService.findAllWithPagination(
                filter.getPageRequest(),
                filter.getLanguage(),
                filter.getCurrentUserId(),
                filter.getType(),
                filter.getUserId(),
                filter.getIsVerified(),
                filter.isShortInfo(),
                filter.isPreview()
            );
        }
    }
}

