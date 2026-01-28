package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.io.File;

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
    
    /**
     * Получает фото профиля пользователя через Telegram Bot API
     * Результат кэшируется в Caffeine на 15 минут
     * 
     * @param userId ID пользователя в Telegram
     * @return JSON объект с информацией о фото профиля или null если ошибка
     */
    @Cacheable(value = "userProfilePhotos", key = "#userId", unless = "#result == null")
    public Object getUserProfilePhotos(Long userId) {
        try {
            LOGGER.debug("🔍 Получение фото профиля пользователя '{}' (запрос к Telegram API)", userId);
            
            // Получаем токен бота из конфигурации
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }
            
            // Формируем URL для запроса getUserProfilePhotos
            String url = TELEGRAM_API_URL + botToken + "/getUserProfilePhotos?user_id=" + userId + "&limit=1";
            
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
                    
                    LOGGER.debug("✅ Фото профиля пользователя '{}' успешно получены", userId);
                    return result;
                } else {
                    String errorDescription = responseJson.has("description") 
                        ? responseJson.get("description").asText() 
                        : "Unknown error";
                    LOGGER.warn("❌ Ошибка от Telegram Bot API для фото пользователя '{}': {}", userId, errorDescription);
                    // Не выбрасываем исключение - просто возвращаем null
                    return null;
                }
            } else {
                LOGGER.warn("❌ Неуспешный HTTP ответ: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось получить фото профиля для пользователя '{}': {} - возвращаем null", 
                    userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Очищает кэш фото профиля для конкретного пользователя
     * 
     * @param userId ID пользователя в Telegram
     */
    @CacheEvict(value = "userProfilePhotos", key = "#userId")
    public void evictUserProfilePhotosCache(Long userId) {
        LOGGER.info("🗑️ Очистка кэша фото профиля для пользователя '{}'", userId);
    }
    
    /**
     * Очищает весь кэш фото профилей
     */
    @CacheEvict(value = "userProfilePhotos", allEntries = true)
    public void evictAllUserProfilePhotosCache() {
        LOGGER.info("🗑️ Очистка всего кэша фото профилей");
    }
    
    /**
     * Проверяет существование стикерсета в Telegram и возвращает его информацию
     * Используется для валидации перед добавлением в базу данных
     * 
     * @param stickerSetName имя стикерсета
     * @return объект с информацией о стикерсете или null если стикерсет не существует
     * @throws RuntimeException если произошла ошибка при обращении к API
     */
    public Object validateStickerSetExists(String stickerSetName) {
        try {
            LOGGER.debug("🔍 Валидация существования стикерсета '{}' в Telegram", stickerSetName);
            
            // Используем существующий метод getStickerSetInfo, который уже кэшируется
            Object stickerSetInfo = getStickerSetInfo(stickerSetName);
            
            if (stickerSetInfo != null) {
                LOGGER.debug("✅ Стикерсет '{}' существует в Telegram", stickerSetName);
                return stickerSetInfo;
            } else {
                LOGGER.warn("❌ Стикерсет '{}' не найден в Telegram", stickerSetName);
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при валидации стикерсета '{}': {}", stickerSetName, e.getMessage());
            throw new RuntimeException("Ошибка при проверке существования стикерсета: " + e.getMessage(), e);
        }
    }
    
    /**
     * Извлекает title из информации о стикерсете, полученной от Telegram API
     * 
     * @param stickerSetInfo информация о стикерсете от Telegram API
     * @return title стикерсета или null если не найден
     */
    public String extractTitleFromStickerSetInfo(Object stickerSetInfo) {
        if (stickerSetInfo == null) {
            return null;
        }
        
        try {
            // Преобразуем в JsonNode для удобного доступа к полям
            JsonNode jsonNode = objectMapper.valueToTree(stickerSetInfo);
            
            if (jsonNode.has("title")) {
                String title = jsonNode.get("title").asText();
                LOGGER.debug("📝 Извлечен title из Telegram API: '{}'", title);
                return title;
            } else {
                LOGGER.warn("⚠️ Поле 'title' не найдено в информации о стикерсете");
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при извлечении title из информации о стикерсете: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Извлекает количество стикеров из информации о стикерсете, полученной от Telegram API
     * 
     * @param stickerSetInfo информация о стикерсете от Telegram API
     * @return количество стикеров или 0 если массив отсутствует или пуст
     */
    public Integer extractStickersCountFromStickerSetInfo(Object stickerSetInfo) {
        if (stickerSetInfo == null) {
            return 0;
        }
        
        try {
            // Преобразуем в JsonNode для удобного доступа к полям
            JsonNode jsonNode = objectMapper.valueToTree(stickerSetInfo);
            
            if (jsonNode.has("stickers") && jsonNode.get("stickers").isArray()) {
                int count = jsonNode.get("stickers").size();
                LOGGER.debug("📊 Извлечено количество стикеров из Telegram API: {}", count);
                return count;
            } else {
                LOGGER.warn("⚠️ Поле 'stickers' не найдено или не является массивом в информации о стикерсете");
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при извлечении количества стикеров из информации о стикерсете: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Создает новый стикерсет в Telegram
     * 
     * @param userId ID пользователя в Telegram
     * @param stickerFile файл стикера (PNG)
     * @param name имя стикерсета
     * @param title название стикерсета
     * @param emoji эмодзи для стикера
     * @return true если успешно создан
     */
    @CacheEvict(value = "stickerSetInfo", key = "#name")
    public boolean createNewStickerSet(Long userId, File stickerFile, String name, String title, String emoji) {
        try {
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }

            String url = TELEGRAM_API_URL + botToken + "/createNewStickerSet";
            
            LOGGER.info("🎯 Создаем стикерсет: {} | Title: {} | UserId: {} | Emoji: {}", name, title, userId, emoji);
            LOGGER.info("📁 Файл стикера: {} | Размер: {} bytes | Существует: {}", 
                    stickerFile.getAbsolutePath(), stickerFile.length(), stickerFile.exists());
            
            // Подготавливаем данные для отправки
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("user_id", userId.toString());
            body.add("name", name);
            body.add("title", title);
            
            // Используем PNG формат (стабильное решение)
            body.add("png_sticker", new FileSystemResource(stickerFile));
            LOGGER.info("📎 Используем PNG формат для стикера");
            
            body.add("emojis", emoji);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            LOGGER.info("🚀 Отправляем запрос к Telegram API: createNewStickerSet | Body keys: {}", body.keySet());
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            
            String responseBody = response.getBody();
            LOGGER.info("🚀 Отправлен запрос к Telegram API: createNewStickerSet | Status: {} | Response length: {}", 
                    response.getStatusCode(), responseBody != null ? responseBody.length() : 0);
            
            LOGGER.info("📦 Ответ от Telegram (createNewStickerSet): {}", responseBody);
            
            boolean success = responseBody != null && responseBody.contains("\"ok\":true");
            if (success) {
                // Очищаем кэш для этого стикерсета
                evictStickerSetCache(name);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании стикерсета: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Добавляет стикер к существующему стикерсету
     * 
     * @param userId ID пользователя в Telegram
     * @param stickerFile файл стикера (PNG)
     * @param name имя стикерсета
     * @param emoji эмодзи для стикера
     * @return true если успешно добавлен
     */
    @CacheEvict(value = "stickerSetInfo", key = "#name")
    public boolean addStickerToSet(Long userId, File stickerFile, String name, String emoji) {
        try {
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }

            String url = TELEGRAM_API_URL + botToken + "/addStickerToSet";
            
            LOGGER.info("➕ Добавляем стикер к стикерсету: {} | UserId: {} | Emoji: {}", name, userId, emoji);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("user_id", userId.toString());
            body.add("name", name);
            
            // Используем PNG формат (стабильное решение)
            body.add("png_sticker", new FileSystemResource(stickerFile));
            LOGGER.info("📎 Используем PNG формат для добавления стикера");
            
            body.add("emojis", emoji);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            LOGGER.info("🚀 Отправляем запрос к Telegram API: addStickerToSet | Body keys: {}", body.keySet());
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            
            String responseBody = response.getBody();
            LOGGER.info("🚀 Отправлен запрос к Telegram API: addStickerToSet | Status: {} | Response length: {}", 
                    response.getStatusCode(), responseBody != null ? responseBody.length() : 0);
            
            LOGGER.info("📦 Ответ от Telegram (addStickerToSet): {}", responseBody);
            
            boolean success = responseBody != null && responseBody.contains("\"ok\":true");
            if (success) {
                // Очищаем кэш для этого стикерсета
                evictStickerSetCache(name);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при добавлении стикера: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Удаляет стикер из стикерсета
     * 
     * @param userId ID пользователя в Telegram
     * @param stickerFileId file_id стикера для удаления
     * @return true если успешно удален
     */
    @CacheEvict(value = "stickerSetInfo", allEntries = true)
    public boolean deleteStickerFromSet(Long userId, String stickerFileId) {
        try {
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }

            String url = TELEGRAM_API_URL + botToken + "/deleteStickerFromSet";
            
            LOGGER.info("🗑️ Удаляем стикер из стикерсета: fileId={} | UserId: {}", stickerFileId, userId);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("user_id", userId.toString());
            body.add("sticker", stickerFileId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            LOGGER.info("🚀 Отправляем запрос к Telegram API: deleteStickerFromSet");
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            
            String responseBody = response.getBody();
            if (responseBody != null) {
                LOGGER.info("📦 Ответ от Telegram (deleteStickerFromSet): {}", responseBody);
            } else {
                LOGGER.warn("⚠️ Пустой ответ от Telegram API при удалении стикера");
            }
            
            boolean success = responseBody != null && responseBody.contains("\"ok\":true");
            if (success) {
                // Очищаем весь кэш стикерсетов, так как мы не знаем имя стикерсета
                evictAllStickerSetCache();
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении стикера: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Получает информацию о стикерсете и возвращает упрощенный объект StickerSetInfo
     * Используется для проверки существования и получения количества стикеров
     * 
     * @param stickerSetName имя стикерсета
     * @return StickerSetInfo с информацией о стикерсете или null если не найден
     */
    public StickerSetInfo getStickerSetInfoSimple(String stickerSetName) {
        try {
            Object stickerSetInfo = getStickerSetInfo(stickerSetName);
            if (stickerSetInfo == null) {
                return null;
            }
            
            Integer stickerCount = extractStickersCountFromStickerSetInfo(stickerSetInfo);
            return new StickerSetInfo(stickerSetName, stickerCount, true);
            
        } catch (HttpClientErrorException.NotFound e) {
            LOGGER.info("📦 Стикерсет {} не найден в Telegram (404)", stickerSetName);
            return null;
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении информации о стикерсете {}: {}", stickerSetName, e.getMessage());
            return null;
        }
    }

    /**
     * Возвращает список file_id всех стикеров в стикерсете в порядке, который приходит от Telegram.
     *
     * @param stickerSetName имя стикерсета
     * @return список file_id или null, если набор не найден / ошибка
     */
    public java.util.List<String> getStickerFileIdsInOrder(String stickerSetName) {
        try {
            Object stickerSetInfo = getStickerSetInfo(stickerSetName);
            if (stickerSetInfo == null) {
                return null;
            }

            JsonNode root = objectMapper.valueToTree(stickerSetInfo);
            if (!root.has("stickers") || !root.get("stickers").isArray()) {
                return null;
            }

            JsonNode stickers = root.get("stickers");
            java.util.List<String> result = new java.util.ArrayList<>();
            for (JsonNode sticker : stickers) {
                JsonNode fileIdNode = sticker.get("file_id");
                if (fileIdNode != null && !fileIdNode.isNull()) {
                    result.add(fileIdNode.asText());
                }
            }

            return result;
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении списка file_id стикеров: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Получает file_id стикера по индексу из стикерсета.
     * Берёт именно основной field result.stickers[stickerIndex].file_id,
     * игнорируя thumbnail/thumb и другие вложенные file_id.
     *
     * @param stickerSetName имя стикерсета
     * @param stickerIndex   индекс стикера (0-based)
     * @return file_id стикера или null если не найден
     */
    public String getStickerFileId(String stickerSetName, int stickerIndex) {
        try {
            LOGGER.info("🔍 Получаем file_id стикера из стикерсета: '{}' | Индекс: {}", stickerSetName, stickerIndex);

            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }

            String url = TELEGRAM_API_URL + botToken + "/getStickerSet?name=" + stickerSetName;
            LOGGER.info("🌐 Запрос к Telegram API: {}", url.replace(botToken, "***"));

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            String responseBody = response.getBody();
            LOGGER.info("📬 Ответ от Telegram API: Status={} | Body={}",
                    response.getStatusCode(),
                    responseBody != null ? responseBody.substring(0, Math.min(200, responseBody.length())) + "..." : "null");

            if (!response.getStatusCode().is2xxSuccessful() || responseBody == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(responseBody);
            if (!root.has("ok") || !root.get("ok").asBoolean()) {
                return null;
            }

            JsonNode result = root.get("result");
            if (result == null || !result.has("stickers")) {
                return null;
            }

            JsonNode stickers = result.get("stickers");
            if (!stickers.isArray() || stickers.size() <= stickerIndex) {
                LOGGER.warn("⚠️ Стикер с индексом {} не найден в массиве stickers", stickerIndex);
                return null;
            }

            JsonNode sticker = stickers.get(stickerIndex);
            JsonNode fileIdNode = sticker.get("file_id");
            if (fileIdNode == null || fileIdNode.isNull()) {
                LOGGER.warn("⚠️ Поле file_id у стикера с индексом {} отсутствует", stickerIndex);
                return null;
            }

            String fileId = fileIdNode.asText();
            LOGGER.info("✅ Найден основной file_id стикера: {}", fileId);
            return fileId;

        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении file_id стикера: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Создает invoice link для оплаты Telegram Stars
     * 
     * @param title название товара
     * @param description описание товара
     * @param payload уникальный payload для связи invoice с заказом
     * @param currency валюта (для Stars используется "XTR")
     * @param prices список цен (обычно один элемент с label и amount)
     * @return URL invoice для оплаты или null при ошибке
     */
    public String createInvoiceLink(String title, String description, String payload, String currency, java.util.List<LabeledPrice> prices) {
        try {
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.warn("⚠️ Токен бота не настроен в конфигурации");
                throw new IllegalArgumentException("Токен бота не настроен");
            }

            String url = TELEGRAM_API_URL + botToken + "/createInvoiceLink";
            
            LOGGER.info("💳 Создаем invoice link: title={}, payload={}, currency={}, prices={}", 
                    title, payload, currency, prices.size());
            
            // Формируем JSON body
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("title", title);
            requestBody.put("description", description);
            requestBody.put("payload", payload);
            requestBody.put("currency", currency);
            
            // Преобразуем prices в список Map
            java.util.List<java.util.Map<String, Object>> pricesList = new java.util.ArrayList<>();
            for (LabeledPrice price : prices) {
                java.util.Map<String, Object> priceMap = new java.util.HashMap<>();
                priceMap.put("label", price.getLabel());
                priceMap.put("amount", price.getAmount());
                pricesList.add(priceMap);
            }
            requestBody.put("prices", pricesList);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
            
            LOGGER.debug("🌐 Отправляем запрос к Telegram API: createInvoiceLink | Body: {}", jsonBody);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            
            String responseBody = response.getBody();
            LOGGER.info("📦 Ответ от Telegram (createInvoiceLink): Status={}, Response length={}", 
                    response.getStatusCode(), responseBody != null ? responseBody.length() : 0);
            
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                
                if (responseJson.has("ok") && responseJson.get("ok").asBoolean()) {
                    String invoiceUrl = responseJson.get("result").asText();
                    LOGGER.info("✅ Invoice link создан успешно: {}", invoiceUrl);
                    return invoiceUrl;
                } else {
                    String errorDescription = responseJson.has("description") 
                        ? responseJson.get("description").asText() 
                        : "Unknown error";
                    LOGGER.error("❌ Ошибка от Telegram Bot API при создании invoice: {}", errorDescription);
                    throw new RuntimeException("Telegram API error: " + errorDescription);
                }
            } else {
                LOGGER.error("❌ Неуспешный HTTP ответ: {}", response.getStatusCode());
                throw new RuntimeException("HTTP error: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            LOGGER.error("❌ Ошибка сетевого запроса к Telegram Bot API при создании invoice: {}", e.getMessage());
            throw new RuntimeException("Network error while creating invoice link", e);
        } catch (Exception e) {
            LOGGER.error("❌ Неожиданная ошибка при создании invoice link: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error while creating invoice link", e);
        }
    }
    
    /**
     * Класс для представления цены в invoice
     */
    public static class LabeledPrice {
        private String label;
        private Integer amount;
        
        public LabeledPrice(String label, Integer amount) {
            this.label = label;
            this.amount = amount;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public Integer getAmount() {
            return amount;
        }
        
        public void setAmount(Integer amount) {
            this.amount = amount;
        }
    }

    /**
     * Информация о стикерсете (упрощенная версия)
     */
    public static class StickerSetInfo {
        private final String name;
        private final int stickerCount;
        private final boolean exists;
        
        public StickerSetInfo(String name, int stickerCount, boolean exists) {
            this.name = name;
            this.stickerCount = stickerCount;
            this.exists = exists;
        }
        
        public String getName() { return name; }
        public int getStickerCount() { return stickerCount; }
        public boolean exists() { return exists; }
    }
}
