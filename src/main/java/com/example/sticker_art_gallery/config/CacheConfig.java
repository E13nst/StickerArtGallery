package com.example.sticker_art_gallery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Конфигурация кэширования
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Настройка менеджера кэша с использованием Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        Caffeine<Object, Object> defaultBuilder = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .recordStats();

        Caffeine<Object, Object> statisticsBuilder = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .recordStats();

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            new CaffeineCache("stickerSetInfo", defaultBuilder.build()),
            new CaffeineCache("userInfo", defaultBuilder.build()),
            new CaffeineCache("userProfilePhotos", defaultBuilder.build()),
            new CaffeineCache("artRules", defaultBuilder.build()),
            new CaffeineCache("serviceStatistics", statisticsBuilder.build())
        ));

        return cacheManager;
    }
}
