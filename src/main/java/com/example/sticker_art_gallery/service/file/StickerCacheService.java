package com.example.sticker_art_gallery.service.file;

import com.example.sticker_art_gallery.dto.StickerCacheDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Сервис для кэширования стикеров в Redis
 */
@Service
public class StickerCacheService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerCacheService.class);
    private static final String CACHE_KEY_PREFIX = "sticker:file:";
    
    @org.springframework.beans.factory.annotation.Value("${app.sticker-cache.ttl-days:7}")
    private long cacheTtlDays;
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    public StickerCacheService(@Qualifier("stickerRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Получает стикер из кэша по file_id
     * 
     * @param fileId идентификатор файла
     * @return StickerCacheDto или null если не найден
     */
    public StickerCacheDto get(String fileId) {
        LOGGER.debug("🔍 Попытка получить стикер '{}' из кэша", fileId);
        
        try {
            String key = buildCacheKey(fileId);
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached instanceof StickerCacheDto stickerCache) {
                if (stickerCache.isExpired()) {
                    LOGGER.debug("⏰ Кэш стикера '{}' устарел, удаляем", fileId);
                    delete(fileId);
                    return null;
                }
                
                LOGGER.info("🎯 Cache HIT для '{}' (size: {} bytes)", fileId, stickerCache.getFileSize());
                return stickerCache;
            }
            
            LOGGER.debug("❌ Cache MISS для '{}'", fileId);
            return null;
            
        } catch (Exception e) {
            LOGGER.debug("⚠️ Redis недоступен для '{}', пропускаем кэш: {}", fileId, e.getMessage());
            return null; // Graceful degradation - работаем без кеша
        }
    }
    
    /**
     * Сохраняет стикер в кэш
     * 
     * @param stickerCache данные стикера для кэширования
     */
    public void put(StickerCacheDto stickerCache) {
        if (stickerCache == null || stickerCache.getFileId() == null) {
            return; // Игнорируем null
        }
        
        try {
            String key = buildCacheKey(stickerCache.getFileId());
            redisTemplate.opsForValue().set(key, stickerCache, cacheTtlDays, TimeUnit.DAYS);
            LOGGER.debug("💾 Кэш для '{}' (size: {} bytes, TTL: {} days)", 
                    stickerCache.getFileId(), stickerCache.getFileSize(), cacheTtlDays);
            
        } catch (Exception e) {
            LOGGER.debug("⚠️ Redis недоступен, пропускаем кэш для '{}': {}", 
                    stickerCache.getFileId(), e.getMessage());
            // Graceful degradation - продолжаем без кеша
        }
    }
    
    /**
     * Удаляет стикер из кэша
     * 
     * @param fileId идентификатор файла
     */
    public void delete(String fileId) {
        try {
            String key = buildCacheKey(fileId);
            redisTemplate.delete(key);
        } catch (Exception e) {
            LOGGER.debug("⚠️ Redis недоступен для delete('{}'): {}", fileId, e.getMessage());
        }
    }
    
    /**
     * Проверяет существование стикера в кэше
     * 
     * @param fileId идентификатор файла
     * @return true если стикер существует в кэше
     */
    public boolean exists(String fileId) {
        try {
            String key = buildCacheKey(fileId);
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Получает размер кэша (количество стикеров)
     * 
     * @return количество закэшированных стикеров
     */
    public long getCacheSize() {
        try {
            String pattern = CACHE_KEY_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
            
        } catch (Exception e) {
            LOGGER.debug("⚠️ Redis недоступен для getCacheSize: {}", e.getMessage());
            return -1;
        }
    }
    
    /**
     * Очищает весь кэш стикеров
     */
    public void clearAll() {
        try {
            var keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            LOGGER.debug("⚠️ Redis недоступен для clearAll: {}", e.getMessage());
        }
    }
    
    /**
     * Создает ключ для Redis
     */
    private String buildCacheKey(String fileId) {
        return CACHE_KEY_PREFIX + fileId;
    }
    
    /**
     * Проверяет доступность Redis
     * 
     * @deprecated Удалено - больше не нужна проверка. Redis просто используем в try-catch
     */
    @Deprecated
    public boolean isRedisAvailable() {
        // Legacy метод - оставлен для обратной совместимости, но больше не используется
        return true;
    }
    
    /**
     * Получает ЛЕГКОВЕСНУЮ статистику кеша (только общие метрики, без обхода Redis)
     * 
     * ⚠️ СУПЕР БЫСТРО: Не делает SCAN/KEYS, использует только метрики
     */
    public java.util.Map<String, Object> getDetailedStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        try {
            stats.put("available", true);
            stats.put("note", "Легковесная статистика без обхода ключей Redis. Используйте метрики для детальной информации.");
            
        } catch (Exception e) {
            stats.put("available", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
}
