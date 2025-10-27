package com.example.sticker_art_gallery.service.proxy;

import com.example.sticker_art_gallery.dto.StickerCacheDto;
import com.example.sticker_art_gallery.service.file.StickerCacheService;
import com.example.sticker_art_gallery.service.metrics.StickerProxyMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * Сервис для проксирования запросов к внешнему сервису стикеров с кэшированием в Redis
 */
@Service
public class StickerProxyService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerProxyService.class);
    
    @Value("${STICKER_PROCESSOR_URL}")
    private String stickerProcessorUrl;
    
    @Value("${app.sticker-cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${app.sticker-cache.ttl-days:7}")
    private long cacheTtlDays;
    
    @Value("${app.sticker-cache.min-size-bytes:1024}")
    private long cacheMinSizeBytes;
    
    @Value("${app.sticker-cache.compress.enabled:false}")
    private boolean cacheCompressEnabled;
    
    @Value("${app.sticker-cache.compress.compress-by-size:100000}")
    private long cacheCompressMinSize;
    
    private final RestTemplate restTemplate;
    private final StickerProxyMetrics metrics;
    private final StickerCacheService cacheService;
    
    @Autowired
    public StickerProxyService(RestTemplate restTemplate, 
                               StickerProxyMetrics metrics,
                               StickerCacheService cacheService) {
        this.restTemplate = restTemplate;
        this.metrics = metrics;
        this.cacheService = cacheService;
    }
    
    /**
     * Получает стикер с кэшированием в Redis
     */
    public ResponseEntity<Object> getSticker(String fileId) {
        LOGGER.info("🔍 Получение стикера '{}' через прокси (cache enabled: {})", fileId, cacheEnabled);
        metrics.incrementTotalRequests();
        
        // 1. Проверяем кеш, если включен
        if (cacheEnabled) {
            StickerCacheDto cached = cacheService.get(fileId);
            if (cached != null) {
                LOGGER.info("🎯 Cache HIT для '{}' (size: {} bytes, age: {} days)", 
                           fileId, cached.getFileSize(), 
                           java.time.Duration.between(cached.getCachedAt(), LocalDateTime.now()).toDays());
                metrics.incrementCacheHits();
                
                return buildResponseFromCache(cached);
            }
            
            LOGGER.debug("❌ Cache MISS для '{}'", fileId);
            metrics.incrementCacheMisses();
        } else {
            LOGGER.debug("⚠️ Кеширование отключено, пропускаем проверку кеша");
        }
        
        // 2. Загружаем из внешнего API
        try {
            String url = stickerProcessorUrl + "/stickers/" + fileId;
            LOGGER.debug("🌐 Проксируем запрос к: {}", url);
            metrics.incrementProxyRequests();
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            long duration = System.currentTimeMillis() - startTime;
            
            byte[] data = response.getBody();
            String contentType = response.getHeaders().getFirst("Content-Type");
            
            LOGGER.info("✅ Прокси-запрос выполнен: fileId={}, status={}, size={} bytes, duration={} ms", 
                       fileId, response.getStatusCode(), data != null ? data.length : 0, duration);
            
            // 3. Кешируем результат, если нужно
            if (cacheEnabled && data != null && shouldCache(data.length)) {
                cacheResult(fileId, data, contentType);
            } else if (data != null) {
                LOGGER.debug("⚠️ Файл '{}' не кешируется (размер: {} bytes, min: {} bytes)", 
                            fileId, data.length, cacheMinSizeBytes);
            }
            
            // 4. Возвращаем результат
            return buildResponse(data, contentType);
            
        } catch (HttpClientErrorException e) {
            LOGGER.warn("⚠️ Клиентская ошибка при проксировании '{}': {} {}", fileId, e.getStatusCode(), e.getMessage());
            metrics.incrementErrors();
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            
        } catch (HttpServerErrorException e) {
            LOGGER.error("❌ Серверная ошибка при проксировании '{}': {} {}", fileId, e.getStatusCode(), e.getMessage());
            metrics.incrementErrors();
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            
        } catch (ResourceAccessException e) {
            LOGGER.error("❌ Ошибка подключения при проксировании '{}': {}", fileId, e.getMessage());
            metrics.incrementErrors();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Service unavailable");
            
        } catch (Exception e) {
            LOGGER.error("❌ Неожиданная ошибка при проксировании '{}': {}", fileId, e.getMessage(), e);
            metrics.incrementErrors();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
    
    /**
     * Проверяет, нужно ли кешировать файл (по размеру)
     */
    private boolean shouldCache(long fileSize) {
        return fileSize >= cacheMinSizeBytes;
    }
    
    /**
     * Кеширует результат в Redis
     */
    private void cacheResult(String fileId, byte[] data, String contentType) {
        try {
            StickerCacheDto cacheDto = new StickerCacheDto();
            cacheDto.setFileId(fileId);
            cacheDto.setContentType(contentType);
            cacheDto.setFileSize(data.length);
            cacheDto.setCachedAt(LocalDateTime.now());
            cacheDto.setExpiresAt(LocalDateTime.now().plusDays(cacheTtlDays));
            
            // Решаем, нужно ли сжимать файл
            boolean shouldCompress = shouldCompress(data.length, contentType);
            cacheDto.setCompressed(shouldCompress);
            
            cacheDto.setData(data); // Автоматически сожмется если флаг установлен
            
            cacheService.put(cacheDto);
            
            LOGGER.info("💾 Файл '{}' сохранен в кеш (size: {} bytes, TTL: {} days, compressed: {})", 
                       fileId, data.length, cacheTtlDays, shouldCompress);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при кешировании '{}': {}", fileId, e.getMessage());
        }
    }
    
    /**
     * Решает, нужно ли сжимать файл
     * Логика:
     * 1. Если сжатие отключено - НЕ сжимать
     * 2. Если файл меньше минимального размера - НЕ сжимать
     * 3. Иначе - сжимать
     */
    private boolean shouldCompress(long fileSize, String contentType) {
        if (!cacheCompressEnabled) {
            return false; // Сжатие отключено
        }
        
        if (fileSize < cacheCompressMinSize) {
            return false; // Файл слишком маленький для сжатия
        }
        
        // Дополнительная логика: не сжимаем изображения WebP
        if (contentType != null && contentType.contains("image/webp")) {
            return false; // WebP файлы обычно маленькие и уже оптимизированы
        }
        
        return true; // Сжимаем большие TGS и другие файлы
    }
    
    /**
     * Формирует ответ из кеша
     */
    private ResponseEntity<Object> buildResponseFromCache(StickerCacheDto cached) {
        String contentType = cached.getContentType();
        byte[] data = cached.getData();
        
        if (contentType != null && contentType.contains("application/json")) {
            try {
                String jsonString = new String(data, "UTF-8");
                Object jsonObject = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonString, Object.class);
                return ResponseEntity.ok(jsonObject);
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка парсинга JSON из кеша: {}", e.getMessage());
                // Возвращаем бинарные данные как fallback
            }
        }
        
        return ResponseEntity.ok()
                .header("Content-Type", contentType != null ? contentType : "image/webp")
                .header("X-Cache", "HIT")
                .body(data);
    }
    
    /**
     * Формирует ответ из свежих данных
     */
    private ResponseEntity<Object> buildResponse(byte[] data, String contentType) {
        if (contentType != null && contentType.contains("application/json")) {
            try {
                String jsonString = new String(data, "UTF-8");
                Object jsonObject = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonString, Object.class);
                return ResponseEntity.ok()
                        .header("X-Cache", "MISS")
                        .body(jsonObject);
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка парсинга JSON: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("JSON parsing error");
            }
        }
        
        return ResponseEntity.ok()
                .header("Content-Type", contentType != null ? contentType : "image/webp")
                .header("X-Cache", "MISS")
                .body(data);
    }
    
    /**
     * Получает статистику кэша (простое проксирование)
     */
    public ResponseEntity<Object> getCacheStats() {
        LOGGER.info("📊 Получение статистики кэша через прокси");
        metrics.incrementCacheStatsRequests();
        
        try {
            String url = stickerProcessorUrl + "/cache/stats";
            LOGGER.debug("🌐 Проксируем запрос статистики к: {}", url);
            
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            
            LOGGER.info("✅ Прокси-запрос статистики выполнен: status={}", response.getStatusCode());
            return response;
            
        } catch (HttpClientErrorException e) {
            LOGGER.warn("⚠️ Клиентская ошибка при получении статистики: {} {}", e.getStatusCode(), e.getMessage());
            metrics.incrementCacheStatsErrors();
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            
        } catch (HttpServerErrorException e) {
            LOGGER.error("❌ Серверная ошибка при получении статистики: {} {}", e.getStatusCode(), e.getMessage());
            metrics.incrementCacheStatsErrors();
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
            
        } catch (ResourceAccessException e) {
            LOGGER.error("❌ Ошибка подключения при получении статистики: {}", e.getMessage());
            metrics.incrementCacheStatsErrors();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Service unavailable");
            
        } catch (Exception e) {
            LOGGER.error("❌ Неожиданная ошибка при получении статистики: {}", e.getMessage(), e);
            metrics.incrementCacheStatsErrors();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
}
