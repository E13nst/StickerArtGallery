package com.example.sticker_art_gallery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Настройка кэша для информации о стикерсетах
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)                    // Максимум 1000 записей
            .expireAfterWrite(15, TimeUnit.MINUTES) // TTL 15 минут
            .recordStats()                        // Включаем статистику
        );
        
        // Регистрируем кэши
        cacheManager.setCacheNames(java.util.List.of("stickerSetInfo", "userInfo"));
        
        return cacheManager;
    }
}
