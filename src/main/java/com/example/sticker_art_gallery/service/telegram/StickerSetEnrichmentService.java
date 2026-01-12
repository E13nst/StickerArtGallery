package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.service.transaction.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для обогащения стикерсетов данными из Telegram Bot API
 */
@Service
public class StickerSetEnrichmentService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetEnrichmentService.class);
    private final TelegramBotApiService telegramBotApiService;
    private final StickerSetRepository stickerSetRepository;
    private final WalletService walletService;
    
    @Autowired
    public StickerSetEnrichmentService(TelegramBotApiService telegramBotApiService,
                                     StickerSetRepository stickerSetRepository,
                                     WalletService walletService) {
        this.telegramBotApiService = telegramBotApiService;
        this.stickerSetRepository = stickerSetRepository;
        this.walletService = walletService;
    }
    
    /**
     * Обогащает список стикерсетов данными из Bot API и категориями
     */
    public List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId, boolean shortInfo, boolean preview, boolean includeAvailableActions) {
        if (stickerSets.isEmpty()) {
            return List.of();
        }
        
        LOGGER.debug("🚀 Обогащение {} стикерсетов данными Bot API и категориями (последовательно, shortInfo={}, preview={}, includeAvailableActions={})", stickerSets.size(), shortInfo, preview, includeAvailableActions);
        
        // Обрабатываем последовательно, чтобы избежать проблем с Hibernate Session
        List<StickerSetDto> result = stickerSets.stream()
                .map(stickerSet -> enrichSingleStickerSetSafelyWithCategories(stickerSet, language, currentUserId, shortInfo, preview, includeAvailableActions))
                .collect(Collectors.toList());
        
        LOGGER.debug("✅ Обогащение завершено для {} стикерсетов", result.size());
        return result;
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API и категориями (безопасно)
     * @param preview если true, возвращает только 1 случайный стикер вместо полного списка
     * @param includeAvailableActions если true, вычисляет доступные действия для стикерсета
     */
    public StickerSetDto enrichSingleStickerSetSafelyWithCategories(StickerSet stickerSet, String language, Long currentUserId, boolean shortInfo, boolean preview, boolean includeAvailableActions) {
        boolean isAdmin = isCurrentUserAdmin();
        boolean hasTonWallet = false;
        if (currentUserId != null && includeAvailableActions) {
            try {
                hasTonWallet = walletService.hasActiveWallet(currentUserId);
            } catch (Exception e) {
                LOGGER.debug("⚠️ Ошибка при проверке наличия кошелька для пользователя {}: {}", currentUserId, e.getMessage());
                hasTonWallet = false;
            }
        }
        LOGGER.debug("🔍 Обогащение стикерсета {}: currentUserId={}, stickerSetUserId={}, isAdmin={}, preview={}, includeAvailableActions={}, hasTonWallet={}", 
                stickerSet.getId(), currentUserId, stickerSet.getUserId(), isAdmin, preview, includeAvailableActions, hasTonWallet);
        StickerSetDto dto = StickerSetDto.fromEntity(stickerSet, language, currentUserId, isAdmin, includeAvailableActions, hasTonWallet);
        
        if (dto == null) {
            LOGGER.warn("⚠️ Не удалось создать DTO для стикерсета {}", stickerSet.getId());
            return null;
        }
        
        LOGGER.debug("🔍 Результат обогащения стикерсета {}: availableActions={}", 
                stickerSet.getId(), dto.getAvailableActions());
        
        if (shortInfo) {
            dto.setTelegramStickerSetInfo(null);
            return dto;
        }
        
        Object botApiData = null;
        try {
            botApiData = telegramBotApiService.getStickerSetInfo(stickerSet.getName());
            
            // Применяем фильтрацию для режима превью
            if (preview && botApiData != null) {
                botApiData = filterStickersForPreview(botApiData);
            }
            
            dto.setTelegramStickerSetInfo(botApiData);
            LOGGER.debug("✅ Стикерсет '{}' обогащен данными Bot API (preview={})", stickerSet.getName(), preview);
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось получить данные Bot API для стикерсета '{}': {} - пропускаем обогащение", 
                    stickerSet.getName(), e.getMessage());
            // Оставляем telegramStickerSetInfo = null, продолжаем обработку
            dto.setTelegramStickerSetInfo(null);
        }
        
        return dto;
    }
    
    /**
     * Фильтрует стикеры в telegramStickerSetInfo для режима превью.
     * Оставляет только 1 случайный стикер из полного списка.
     */
    private Object filterStickersForPreview(Object telegramStickerSetInfo) {
        if (telegramStickerSetInfo instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> infoMap = new java.util.LinkedHashMap<>((java.util.Map<String, Object>) telegramStickerSetInfo);
            
            Object stickersObj = infoMap.get("stickers");
            if (stickersObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> stickers = (java.util.List<Object>) stickersObj;
                
                if (stickers.size() > 1) {
                    // Выбираем 1 случайный стикер
                    java.util.List<Object> shuffled = new java.util.ArrayList<>(stickers);
                    java.util.Collections.shuffle(shuffled);
                    infoMap.put("stickers", shuffled.subList(0, 1));
                    LOGGER.debug("🎲 Фильтрация стикеров для превью: {} -> 1 случайный", stickers.size());
                }
            }
            return infoMap;
        }
        return telegramStickerSetInfo;
    }
    
    /**
     * Обновляет title и количество стикеров в БД используя данные Telegram API
     * Вызывается только для одного стикерсета после обогащения
     * 
     * @param stickerSet стикерсет для обновления
     * @param botApiData данные Telegram API, уже полученные при обогащении
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTitleAndStickersCount(StickerSet stickerSet, Object botApiData) {
        if (botApiData == null) {
            LOGGER.debug("⚠️ botApiData == null, пропускаем обновление title и stickers_count для стикерсета {}", stickerSet.getId());
            return;
        }
        
        try {
            // Извлекаем актуальные данные из Telegram API
            String newTitle = telegramBotApiService.extractTitleFromStickerSetInfo(botApiData);
            Integer newStickersCount = telegramBotApiService.extractStickersCountFromStickerSetInfo(botApiData);
            
            if (newTitle == null && newStickersCount == null) {
                LOGGER.warn("⚠️ Не удалось извлечь title и stickers_count из botApiData для стикерсета {}", stickerSet.getId());
                return;
            }
            
            boolean needsUpdate = false;
            
            // Проверяем и обновляем title
            if (newTitle != null && !newTitle.equals(stickerSet.getTitle())) {
                LOGGER.debug("📝 Обновление title для стикерсета {}: '{}' -> '{}'", 
                        stickerSet.getId(), stickerSet.getTitle(), newTitle);
                stickerSet.setTitle(newTitle);
                needsUpdate = true;
            }
            
            // Проверяем и обновляем stickers_count
            if (newStickersCount != null && !newStickersCount.equals(stickerSet.getStickersCount())) {
                LOGGER.debug("📊 Обновление stickers_count для стикерсета {}: {} -> {}", 
                        stickerSet.getId(), stickerSet.getStickersCount(), newStickersCount);
                stickerSet.setStickersCount(newStickersCount);
                needsUpdate = true;
            }
            
            // Обновляем в БД только если что-то изменилось
            if (needsUpdate) {
                stickerSetRepository.save(stickerSet);
                LOGGER.info("✅ Обновлены title и/или stickers_count для стикерсета {}", stickerSet.getId());
            } else {
                LOGGER.debug("✓ Данные title и stickers_count актуальны для стикерсета {}", stickerSet.getId());
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении title и stickers_count для стикерсета {}: {}", 
                    stickerSet.getId(), e.getMessage(), e);
            // Не прерываем выполнение - ошибка обновления не должна влиять на возврат DTO
        }
    }
    
    /**
     * Проверяет, является ли текущий пользователь администратором
     */
    private boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при проверке роли администратора: {}", e.getMessage());
            return false;
        }
    }
}
