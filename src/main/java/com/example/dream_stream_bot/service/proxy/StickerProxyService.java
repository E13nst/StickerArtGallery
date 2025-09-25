package com.example.dream_stream_bot.service.proxy;

import com.example.dream_stream_bot.service.metrics.StickerProxyMetrics;
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

/**
 * Сервис для проксирования запросов к внешнему сервису стикеров
 */
@Service
public class StickerProxyService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerProxyService.class);
    
    // Константы для будущего использования (если понадобится кэширование)
    // private static final String CACHE_KEY_PREFIX = "sticker:file:";
    // private static final long CACHE_TTL_DAYS = 7; // 7 дней
    
    @Value("${STICKER_PROCESSOR_URL}")
    private String stickerProcessorUrl;
    
    private final RestTemplate restTemplate;
    private final StickerProxyMetrics metrics;
    
    @Autowired
    public StickerProxyService(RestTemplate restTemplate, StickerProxyMetrics metrics) {
        this.restTemplate = restTemplate;
        this.metrics = metrics;
    }
    
    /**
     * Получает стикер (простое проксирование без кэширования)
     */
    public ResponseEntity<Object> getSticker(String fileId) {
        LOGGER.info("🔍 Получение стикера '{}' через прокси", fileId);
        metrics.incrementTotalRequests();
        
        // Проксируем запрос к внешнему сервису
        try {
            String url = stickerProcessorUrl + "/stickers/" + fileId;
            LOGGER.debug("🌐 Проксируем запрос к: {}", url);
            metrics.incrementProxyRequests();
            
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            
            LOGGER.info("✅ Прокси-запрос выполнен: fileId={}, status={}, size={} bytes", 
                       fileId, response.getStatusCode(), response.getBody().length);
            
            // Проверяем Content-Type заголовок
            String contentType = response.getHeaders().getFirst("Content-Type");
            LOGGER.debug("📄 Content-Type: {}", contentType);
            
            if (contentType != null && contentType.contains("application/json")) {
                // Это JSON (Lottie анимация) - конвертируем байты в строку и парсим
                try {
                    String jsonString = new String(response.getBody(), "UTF-8");
                    Object jsonObject = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonString, Object.class);
                    return ResponseEntity.ok(jsonObject);
                } catch (Exception e) {
                    LOGGER.error("❌ Ошибка парсинга JSON для '{}': {}", fileId, e.getMessage());
                    metrics.incrementErrors();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("JSON parsing error");
                }
            } else {
                // Это бинарные данные (WebP изображение)
                LOGGER.debug("🖼️ Возвращаем бинарные данные для '{}'", fileId);
                return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "image/webp")
                    .body(response.getBody());
            }
            
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
