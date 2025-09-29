package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

/**
 * Сервис для работы с Telegram Bot API
 */
@Service
public class TelegramBotApiService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotApiService.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    
    private final RestTemplate restTemplate;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TelegramBotApiService(AppConfig appConfig, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Получает информацию о стикерсете через Telegram Bot API
     * Результат кэшируется в Caffeine на 15 минут
     * 
     * @param stickerSetName имя стикерсета
     * @return JSON объект с информацией о стикерсете или null если ошибка
     */
    @Cacheable(value = "stickerSetInfo", key = "#stickerSetName", unless = "#result == null")
    public Object getStickerSetInfo(String stickerSetName) {
        try {
            LOGGER.debug("🔍 Получение информации о стикерсете '{}' (запрос к Telegram API)", stickerSetName);
            
            // Получаем токен бота из конфигурации
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }
            
            // Формируем URL для запроса
            String url = TELEGRAM_API_URL + botToken + "/getStickerSet?name=" + stickerSetName;
            
            LOGGER.debug("🌐 Отправляем запрос к Telegram Bot API: {}", url.replace(botToken, "***"));
            
            // Выполняем запрос
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Парсим ответ и проверяем успешность
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                if (responseJson.has("ok") && responseJson.get("ok").asBoolean()) {
                    // Возвращаем только данные result (без обертки ok, result)
                    JsonNode resultNode = responseJson.get("result");
                    Object result = objectMapper.treeToValue(resultNode, Object.class);
                    
                    LOGGER.debug("✅ Информация о стикерсете '{}' успешно получена", stickerSetName);
                    return result;
                } else {
                    String errorDescription = responseJson.has("description") 
                        ? responseJson.get("description").asText() 
                        : "Unknown error";
                    LOGGER.warn("❌ Ошибка от Telegram Bot API для стикерсета '{}': {}", stickerSetName, errorDescription);
                    throw new RuntimeException("Telegram API error: " + errorDescription);
                }
            } else {
                LOGGER.warn("❌ Неуспешный HTTP ответ: {}", response.getStatusCode());
                throw new RuntimeException("HTTP error: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            LOGGER.error("❌ Ошибка сетевого запроса к Telegram Bot API для стикерсета '{}': {}", stickerSetName, e.getMessage());
            throw new RuntimeException("Network error while fetching sticker set info", e);
        } catch (Exception e) {
            LOGGER.error("❌ Неожиданная ошибка при получении информации о стикерсете '{}': {}", stickerSetName, e.getMessage(), e);
            throw new RuntimeException("Unexpected error while fetching sticker set info", e);
        }
    }
    
    /**
     * Очищает кэш для конкретного стикерсета
     * 
     * @param stickerSetName имя стикерсета
     */
    @CacheEvict(value = "stickerSetInfo", key = "#stickerSetName")
    public void evictStickerSetCache(String stickerSetName) {
        LOGGER.info("🗑️ Очистка кэша для стикерсета '{}'", stickerSetName);
    }
    
    /**
     * Очищает весь кэш стикерсетов
     */
    @CacheEvict(value = "stickerSetInfo", allEntries = true)
    public void evictAllStickerSetCache() {
        LOGGER.info("🗑️ Очистка всего кэша стикерсетов");
    }
    
    /**
     * Получает информацию о пользователе через Telegram Bot API
     * Результат кэшируется в Caffeine на 15 минут
     * 
     * @param userId ID пользователя в Telegram
     * @return JSON объект с информацией о пользователе или null если ошибка
     */
    @Cacheable(value = "userInfo", key = "#userId", unless = "#result == null")
    public Object getUserInfo(Long userId) {
        try {
            LOGGER.debug("🔍 Получение информации о пользователе '{}' (запрос к Telegram API)", userId);
            
            // Получаем токен бота из конфигурации
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }
            
            // Формируем URL для запроса getChatMember
            // Используем getChatMember с chat_id = user_id для получения информации о пользователе
            String url = TELEGRAM_API_URL + botToken + "/getChatMember?chat_id=" + userId + "&user_id=" + userId;
            
            LOGGER.debug("🌐 Отправляем запрос к Telegram Bot API: {}", url.replace(botToken, "***"));
            
            // Выполняем запрос
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Парсим ответ и проверяем успешность
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                if (responseJson.has("ok") && responseJson.get("ok").asBoolean()) {
                    // Возвращаем только данные result (без обертки ok, result)
                    JsonNode resultNode = responseJson.get("result");
                    Object result = objectMapper.treeToValue(resultNode, Object.class);
                    
                    LOGGER.debug("✅ Информация о пользователе '{}' успешно получена", userId);
                    return result;
                } else {
                    String errorDescription = responseJson.has("description") 
                        ? responseJson.get("description").asText() 
                        : "Unknown error";
                    LOGGER.warn("❌ Ошибка от Telegram Bot API для пользователя '{}': {}", userId, errorDescription);
                    throw new RuntimeException("Telegram API error: " + errorDescription);
                }
            } else {
                LOGGER.warn("❌ Неуспешный HTTP ответ: {}", response.getStatusCode());
                throw new RuntimeException("HTTP error: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            LOGGER.error("❌ Ошибка сетевого запроса к Telegram Bot API для пользователя '{}': {}", userId, e.getMessage());
            throw new RuntimeException("Network error while fetching user info", e);
        } catch (Exception e) {
            LOGGER.error("❌ Неожиданная ошибка при получении информации о пользователе '{}': {}", userId, e.getMessage(), e);
            throw new RuntimeException("Unexpected error while fetching user info", e);
        }
    }
    
    /**
     * Очищает кэш для конкретного пользователя
     * 
     * @param userId ID пользователя в Telegram
     */
    @CacheEvict(value = "userInfo", key = "#userId")
    public void evictUserCache(Long userId) {
        LOGGER.info("🗑️ Очистка кэша для пользователя '{}'", userId);
    }
    
    /**
     * Очищает весь кэш пользователей
     */
    @CacheEvict(value = "userInfo", allEntries = true)
    public void evictAllUserCache() {
        LOGGER.info("🗑️ Очистка всего кэша пользователей");
    }
}
