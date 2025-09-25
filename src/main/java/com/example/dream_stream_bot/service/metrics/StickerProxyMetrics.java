package com.example.dream_stream_bot.service.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Простые метрики для мониторинга прокси-запросов
 */
@Component
public class StickerProxyMetrics {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerProxyMetrics.class);
    
    // Счетчики запросов
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong proxyRequests = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    
    // Счетчики для статистики кэша
    private final AtomicLong cacheStatsRequests = new AtomicLong(0);
    private final AtomicLong cacheStatsErrors = new AtomicLong(0);
    
    /**
     * Увеличивает счетчик общих запросов
     */
    public void incrementTotalRequests() {
        long count = totalRequests.incrementAndGet();
        LOGGER.debug("📊 Общее количество запросов: {}", count);
    }
    
    /**
     * Увеличивает счетчик попаданий в кэш
     */
    public void incrementCacheHits() {
        long count = cacheHits.incrementAndGet();
        LOGGER.debug("🎯 Попаданий в кэш: {}", count);
    }
    
    /**
     * Увеличивает счетчик промахов кэша
     */
    public void incrementCacheMisses() {
        long count = cacheMisses.incrementAndGet();
        LOGGER.debug("❌ Промахов кэша: {}", count);
    }
    
    /**
     * Увеличивает счетчик прокси-запросов
     */
    public void incrementProxyRequests() {
        long count = proxyRequests.incrementAndGet();
        LOGGER.debug("🌐 Прокси-запросов: {}", count);
    }
    
    /**
     * Увеличивает счетчик ошибок
     */
    public void incrementErrors() {
        long count = errors.incrementAndGet();
        LOGGER.warn("❌ Ошибок: {}", count);
    }
    
    /**
     * Увеличивает счетчик запросов статистики кэша
     */
    public void incrementCacheStatsRequests() {
        long count = cacheStatsRequests.incrementAndGet();
        LOGGER.debug("📊 Запросов статистики кэша: {}", count);
    }
    
    /**
     * Увеличивает счетчик ошибок статистики кэша
     */
    public void incrementCacheStatsErrors() {
        long count = cacheStatsErrors.incrementAndGet();
        LOGGER.warn("❌ Ошибок статистики кэша: {}", count);
    }
    
    /**
     * Получает статистику метрик
     */
    public MetricsStats getStats() {
        long total = totalRequests.get();
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long proxy = proxyRequests.get();
        long errorCount = errors.get();
        long cacheStats = cacheStatsRequests.get();
        long cacheStatsErrorCount = cacheStatsErrors.get();
        
        double hitRate = total > 0 ? (double) hits / total * 100 : 0.0;
        
        return new MetricsStats(
            total, hits, misses, proxy, errorCount, 
            cacheStats, cacheStatsErrorCount, hitRate
        );
    }
    
    /**
     * Логирует текущую статистику
     */
    public void logStats() {
        MetricsStats stats = getStats();
        LOGGER.info("📊 Статистика прокси-запросов:");
        LOGGER.info("   Общие запросы: {}", stats.totalRequests());
        LOGGER.info("   Попадания в кэш: {} ({}%)", stats.cacheHits(), String.format("%.2f", stats.hitRate()));
        LOGGER.info("   Промахи кэша: {}", stats.cacheMisses());
        LOGGER.info("   Прокси-запросы: {}", stats.proxyRequests());
        LOGGER.info("   Ошибки: {}", stats.errors());
        LOGGER.info("   Запросы статистики кэша: {}", stats.cacheStatsRequests());
        LOGGER.info("   Ошибки статистики кэша: {}", stats.cacheStatsErrors());
    }
    
    /**
     * Сбрасывает все счетчики
     */
    public void reset() {
        totalRequests.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        proxyRequests.set(0);
        errors.set(0);
        cacheStatsRequests.set(0);
        cacheStatsErrors.set(0);
        LOGGER.info("🔄 Метрики сброшены");
    }
    
    /**
     * DTO для статистики метрик
     */
    public record MetricsStats(
        long totalRequests,
        long cacheHits,
        long cacheMisses,
        long proxyRequests,
        long errors,
        long cacheStatsRequests,
        long cacheStatsErrors,
        double hitRate
    ) {}
}
