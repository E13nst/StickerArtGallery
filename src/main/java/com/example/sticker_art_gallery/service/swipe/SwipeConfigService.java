package com.example.sticker_art_gallery.service.swipe;

import com.example.sticker_art_gallery.model.swipe.SwipeConfigEntity;
import com.example.sticker_art_gallery.repository.SwipeConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с конфигурацией системы отслеживания свайпов.
 * Singleton - конфигурация всегда имеет id=1.
 */
@Service
@Transactional
public class SwipeConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwipeConfigService.class);
    private static final Long CONFIG_ID = 1L;

    private final SwipeConfigRepository swipeConfigRepository;

    public SwipeConfigService(SwipeConfigRepository swipeConfigRepository) {
        this.swipeConfigRepository = swipeConfigRepository;
    }

    /**
     * Получить активную конфигурацию (кэшируется)
     */
    @Cacheable(cacheNames = "swipeConfig", key = "'active'")
    @Transactional(readOnly = true)
    public SwipeConfigEntity getActiveConfig() {
        LOGGER.debug("📋 Получение активной конфигурации свайпов");
        return swipeConfigRepository.findById(CONFIG_ID)
                .orElseThrow(() -> {
                    LOGGER.error("❌ Конфигурация свайпов не найдена в БД (id={})", CONFIG_ID);
                    return new IllegalStateException("Конфигурация свайпов не найдена. Убедитесь, что миграция выполнена.");
                });
    }

    /**
     * Обновить конфигурацию (очищает кэш)
     */
    @CacheEvict(cacheNames = "swipeConfig", allEntries = true)
    public SwipeConfigEntity updateConfig(SwipeConfigEntity config) {
        if (config.getId() == null) {
            config.setId(CONFIG_ID);
        }
        if (!CONFIG_ID.equals(config.getId())) {
            throw new IllegalArgumentException("ID конфигурации должен быть равен " + CONFIG_ID);
        }

        SwipeConfigEntity saved = swipeConfigRepository.save(config);
        LOGGER.info("✅ Конфигурация свайпов обновлена: swipesPerReward={}, rewardAmount={}, " +
                   "dailyLimitRegular={}, dailyLimitPremium={}, rewardAmountPremium={}, resetType={}",
                saved.getSwipesPerReward(), saved.getRewardAmount(),
                saved.getDailyLimitRegular(), saved.getDailyLimitPremium(),
                saved.getRewardAmountPremium(), saved.getResetType());
        return saved;
    }

    /**
     * Очистить кэш конфигурации
     */
    @CacheEvict(cacheNames = "swipeConfig", allEntries = true)
    public void invalidateCache() {
        LOGGER.debug("♻️ Кэш конфигурации свайпов очищен");
    }
}
