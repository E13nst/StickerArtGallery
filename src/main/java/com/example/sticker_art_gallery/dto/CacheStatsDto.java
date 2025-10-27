package com.example.sticker_art_gallery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для статистики кеша стикеров
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatsDto {
    
    /**
     * Время сбора статистики
     */
    private LocalDateTime timestamp;
    
    /**
     * Включен ли кеш
     */
    private boolean cacheEnabled;
    
    /**
     * Доступность Redis
     */
    private boolean redisAvailable;
    
    /**
     * Общее количество ключей в кеше
     */
    private long totalKeys;
    
    /**
     * Общий размер кеша в байтах
     */
    private long totalSizeBytes;
    
    /**
     * Общий размер кеша (человекочитаемый)
     */
    private String totalSizeHuman;
    
    /**
     * Количество cache hits (из метрик)
     */
    private long cacheHits;
    
    /**
     * Количество cache misses (из метрик)
     */
    private long cacheMisses;
    
    /**
     * Hit rate в процентах
     */
    private double hitRatePercent;
    
    /**
     * Количество запросов к прокси
     */
    private long proxyRequests;
    
    /**
     * Количество ошибок
     */
    private long errors;
    
    /**
     * TTL кеша в днях
     */
    private long cacheTtlDays;
    
    /**
     * Минимальный размер файла для кеширования
     */
    private long cacheMinSizeBytes;
    
    /**
     * Средний размер файла в кеше
     */
    private Long avgFileSizeBytes;
    
    /**
     * Минимальный размер файла в кеше
     */
    private Long minFileSizeBytes;
    
    /**
     * Максимальный размер файла в кеше
     */
    private Long maxFileSizeBytes;
    
    /**
     * Количество файлов по размерам (диапазоны)
     */
    private SizeDistribution sizeDistribution;
    
    /**
     * Топ-10 самых больших файлов
     */
    private List<CachedFileInfo> topLargestFiles;
    
    /**
     * Топ-10 самых старых файлов
     */
    private List<CachedFileInfo> topOldestFiles;
    
    /**
     * Распределение по размерам
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeDistribution {
        private long under10KB;   // < 10 KB
        private long under50KB;   // 10-50 KB
        private long under100KB;  // 50-100 KB
        private long under500KB;  // 100-500 KB
        private long over500KB;   // > 500 KB
    }
    
    /**
     * Информация о закешированном файле
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedFileInfo {
        private String fileId;
        private long fileSizeBytes;
        private String fileSizeHuman;
        private LocalDateTime cachedAt;
        private LocalDateTime expiresAt;
        private long ageMinutes;
    }
}

