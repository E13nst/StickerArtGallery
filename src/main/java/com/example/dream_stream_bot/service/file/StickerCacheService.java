package com.example.dream_stream_bot.service.file;

import com.example.dream_stream_bot.dto.StickerCacheDto;
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
    private static final long CACHE_TTL_DAYS = 7; // 7 дней
    
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
        LOGGER.info("🔍 Попытка получить стикер '{}' из кэша", fileId);
        
        if (!isRedisAvailable()) {
            LOGGER.debug("⚠️ Redis недоступен, пропускаем кэш для '{}'", fileId);
            return null;
        }
        
        try {
            String key = buildCacheKey(fileId);
            LOGGER.info("🔑 Ищем в Redis по ключу: {}", key);
            
            Object cached = redisTemplate.opsForValue().get(key);
            LOGGER.info("📦 Результат из Redis: {}", cached != null ? cached.getClass().getSimpleName() : "null");
            
            if (cached instanceof StickerCacheDto stickerCache) {
                if (stickerCache.isExpired()) {
                    LOGGER.debug("⏰ Кэш стикера '{}' устарел, удаляем", fileId);
                    delete(fileId);
                    return null;
                }
                
                LOGGER.info("🎯 Стикер '{}' найден в кэше (размер: {} байт)", fileId, stickerCache.getFileSize());
                return stickerCache;
            }
            
            LOGGER.info("❌ Стикер '{}' не найден в кэше", fileId);
            return null;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении стикера '{}' из кэша: {}", fileId, e.getMessage());
            LOGGER.debug("❌ Полная ошибка get():", e);
            return null;
        }
    }
    
    /**
     * Сохраняет стикер в кэш
     * 
     * @param stickerCache данные стикера для кэширования
     */
    public void put(StickerCacheDto stickerCache) {
        if (stickerCache == null || stickerCache.getFileId() == null) {
            LOGGER.warn("⚠️ Попытка сохранить null стикер в кэш");
            return;
        }
        
        LOGGER.info("💾 Попытка сохранить стикер '{}' в кэш", stickerCache.getFileId());
        
        if (!isRedisAvailable()) {
            LOGGER.debug("⚠️ Redis недоступен, пропускаем сохранение в кэш для '{}'", stickerCache.getFileId());
            return;
        }
        
        try {
            String key = buildCacheKey(stickerCache.getFileId());
            LOGGER.info("🔑 Сохраняем в Redis по ключу: {}", key);
            LOGGER.info("📦 Сохраняем объект: {} (размер: {} байт)", 
                       stickerCache.getClass().getSimpleName(), stickerCache.getFileSize());
            
            // Сохраняем с TTL
            redisTemplate.opsForValue().set(key, stickerCache, CACHE_TTL_DAYS, TimeUnit.DAYS);
            LOGGER.info("✅ Объект сохранен в Redis с TTL {} дней", CACHE_TTL_DAYS);
            
            LOGGER.debug("💾 Стикер '{}' сохранен в кэш (размер: {} байт, TTL: {} дней)", 
                    stickerCache.getFileId(), stickerCache.getFileSize(), CACHE_TTL_DAYS);
            
        } catch (Exception e) {
            LOGGER.warn("❌ Ошибка при сохранении стикера '{}' в кэш: {}", 
                    stickerCache.getFileId(), e.getMessage());
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
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                LOGGER.debug("🗑️ Стикер '{}' удален из кэша", fileId);
            } else {
                LOGGER.debug("❌ Стикер '{}' не найден для удаления", fileId);
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении стикера '{}' из кэша: {}", fileId, e.getMessage(), e);
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
            LOGGER.error("❌ Ошибка при проверке существования стикера '{}': {}", fileId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получает размер кэша (количество стикеров)
     * 
     * @return количество закэшированных стикеров
     */
    public long getCacheSize() {
        LOGGER.debug("🔢 Запрос размера кэша");
        
        if (!isRedisAvailable()) {
            LOGGER.debug("⚠️ Redis недоступен, размер кэша неизвестен");
            return -1;
        }
        
        try {
            String pattern = CACHE_KEY_PREFIX + "*";
            LOGGER.debug("🔍 Ищем ключи по паттерну: {}", pattern);
            var keys = redisTemplate.keys(pattern);
            long size = keys != null ? keys.size() : 0;
            LOGGER.info("📊 Размер кэша: {} ключей", size);
            if (keys != null && !keys.isEmpty()) {
                LOGGER.info("🔑 Найденные ключи: {}", keys);
            }
            return size;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении размера кэша: {}", e.getMessage());
            LOGGER.debug("❌ Полная ошибка getCacheSize:", e);
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
                Long deleted = redisTemplate.delete(keys);
                LOGGER.info("🧹 Очищен кэш стикеров: удалено {} записей", deleted);
            } else {
                LOGGER.info("🧹 Кэш стикеров уже пуст");
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при очистке кэша стикеров: {}", e.getMessage(), e);
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
     */
    public boolean isRedisAvailable() {
        try {
            LOGGER.debug("🔍 Проверяем доступность Redis...");
            // Простая проверка - пытаемся выполнить операцию
            Boolean result = redisTemplate.hasKey("test_key");
            LOGGER.info("✅ Redis доступен! Результат проверки: {}", result);
            return true;
        } catch (Exception e) {
            LOGGER.error("❌ Redis недоступен: {}", e.getMessage());
            LOGGER.debug("❌ Полная ошибка Redis:", e);
            return false;
        }
    }
}
