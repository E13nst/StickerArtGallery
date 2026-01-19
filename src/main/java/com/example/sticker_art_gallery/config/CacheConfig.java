package com.example.sticker_art_gallery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Конфигурация кэширования с оптимизированными размерами для предотвращения утечек памяти
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);
    
    /**
     * Настройка менеджера кэша с использованием Caffeine.
     * Размеры кешей уменьшены для экономии памяти.
     * Используется expireAfterAccess вместо expireAfterWrite для более эффективного использования памяти.
     */
    @Bean
    public CacheManager cacheManager() {
        // Основной кеш для часто используемых данных (уменьшено с 1000 до 200)
        Caffeine<Object, Object> defaultBuilder = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterAccess(10, TimeUnit.MINUTES)  // expireAfterAccess вместо expireAfterWrite
            .recordStats()
            .removalListener((key, value, cause) -> 
                LOGGER.trace("Cache eviction: key={}, cause={}", key, cause));

        // Кеш для редко меняющихся данных (artRules)
        Caffeine<Object, Object> staticDataBuilder = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats();

        // Кеш для статистики (короткое время жизни)
        Caffeine<Object, Object> statisticsBuilder = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .recordStats();

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            new CaffeineCache("stickerSetInfo", defaultBuilder.build()),
            new CaffeineCache("userInfo", defaultBuilder.build()),
            new CaffeineCache("userProfilePhotos", defaultBuilder.build()),
            new CaffeineCache("artRules", staticDataBuilder.build()),
            new CaffeineCache("swipeConfig", staticDataBuilder.build()),
            new CaffeineCache("serviceStatistics", statisticsBuilder.build()),
            new CaffeineCache("userStatistics", statisticsBuilder.build()),
            new CaffeineCache("stickerSetStatistics", statisticsBuilder.build()),
            new CaffeineCache("likeStatistics", statisticsBuilder.build())
        ));

        LOGGER.info("✅ Caffeine cache manager configured with optimized sizes: " +
                   "default=200, staticData=50, statistics=10");

        return cacheManager;
    }
}
