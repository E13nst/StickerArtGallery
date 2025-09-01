package com.example.dream_stream_bot.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.File;

/**
 * Сервис для работы с Telegram Bot API
 */
@Service
public class TelegramApiService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramApiService.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    private String botToken;
    
    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }
    
    /**
     * Создает новый стикерпак в Telegram
     */
    public boolean createNewStickerSet(Long userId, File stickerFile, String name, String title, String emoji) {
        try {
            String url = apiUrl("createNewStickerSet");
            
            LOGGER.info("🎯 Создаем стикерпак: {} | Title: {} | UserId: {} | Emoji: {}", name, title, userId, emoji);
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
            
            LOGGER.info("🚀 Отправлен запрос к Telegram API: createNewStickerSet | Status: {} | Response length: {}", 
                    response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
            
            String responseBody = response.getBody();
            LOGGER.info("📦 Ответ от Telegram (createNewStickerSet): {}", responseBody);
            
            return responseBody != null && responseBody.contains("\"ok\":true");
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании стикерпака: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Добавляет стикер к существующему стикерпаку
     */
    public boolean addStickerToSet(Long userId, File stickerFile, String name, String emoji) {
        try {
            String url = apiUrl("addStickerToSet");
            
            LOGGER.info("➕ Добавляем стикер к стикерпаку: {} | UserId: {} | Emoji: {}", name, userId, emoji);
            
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
            
            LOGGER.info("🚀 Отправлен запрос к Telegram API: addStickerToSet | Status: {} | Response length: {}", 
                    response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
            
            String responseBody = response.getBody();
            LOGGER.info("📦 Ответ от Telegram (addStickerToSet): {}", responseBody);
            
            return responseBody != null && responseBody.contains("\"ok\":true");
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при добавлении стикера: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получает информацию о стикерпаке
     */
    public StickerSetInfo getStickerSetInfo(String stickerSetName) {
        try {
            String url = apiUrl("getStickerSet") + "&name=" + stickerSetName;
            LOGGER.info("🌐 Запрос информации о стикерпаке: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            LOGGER.info("📬 Ответ API: Status={} | Body={}", 
                    response.getStatusCode(), 
                    response.getBody() != null ? response.getBody().substring(0, Math.min(150, response.getBody().length())) + "..." : "null");
            
            if (response.getStatusCode().is2xxSuccessful() && 
                response.getBody() != null && 
                response.getBody().contains("\"ok\":true")) {
                
                // Парсим количество стикеров
                String responseBody = response.getBody();
                int stickerCount = 0;
                if (responseBody.contains("\"stickers\":")) {
                    int stickersStart = responseBody.indexOf("\"stickers\":[");
                    if (stickersStart != -1) {
                        int stickersEnd = responseBody.indexOf("]", stickersStart);
                        if (stickersEnd != -1) {
                            String stickersSection = responseBody.substring(stickersStart, stickersEnd + 1);
                            stickerCount = (stickersSection.split("\"file_id\"").length - 1);
                        }
                    }
                }
                
                return new StickerSetInfo(stickerSetName, stickerCount, true);
            }
            
            return null;
            
        } catch (HttpClientErrorException.NotFound e) {
            LOGGER.info("📦 Стикерпак {} не найден в Telegram (404)", stickerSetName);
            return null;
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении информации о стикерпаке {}: {}", stickerSetName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Получает file_id стикера по индексу из стикерпака
     */
    public String getStickerFileId(String stickerSetName, int stickerIndex) {
        try {
            LOGGER.info("🔍 Получаем file_id стикера из стикерпака: '{}' | Индекс: {}", stickerSetName, stickerIndex);
            
            String url = apiUrl("getStickerSet") + "&name=" + stickerSetName;
            LOGGER.info("🌐 Запрос к Telegram API: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            LOGGER.info("📬 Ответ от Telegram API: Status={} | Body={}", 
                    response.getStatusCode(), 
                    response.getBody() != null ? response.getBody().substring(0, Math.min(200, response.getBody().length())) + "..." : "null");
            
            if (response.getStatusCode().is2xxSuccessful() && 
                response.getBody() != null && 
                response.getBody().contains("\"ok\":true")) {
                
                String responseBody = response.getBody();
                if (responseBody.contains("\"stickers\":")) {
                    int stickersStart = responseBody.indexOf("\"stickers\":[");
                    if (stickersStart != -1) {
                        int stickersEnd = responseBody.indexOf("]", stickersStart);
                        if (stickersEnd != -1) {
                            String stickersSection = responseBody.substring(stickersStart, stickersEnd + 1);
                            
                            // Ищем стикер по индексу - учитываем что у каждого стикера есть file_id и thumbnail.file_id
                            String[] stickers = stickersSection.split("\\{\"width\":");
                            if (stickers.length > stickerIndex + 1) { // +1 потому что первый элемент пустой
                                String targetSticker = stickers[stickerIndex + 1];
                                
                                // Ищем основной file_id стикера (НЕ thumbnail.file_id)
                                // Ищем первый file_id, который НЕ находится внутри блока thumbnail
                                int thumbnailStart = targetSticker.indexOf("\"thumbnail\":");
                                
                                int fileIdStart = targetSticker.indexOf("\"file_id\":\"");
                                
                                // Если file_id находится внутри thumbnail блока, ищем следующий
                                while (fileIdStart != -1) {
                                    // Проверяем, находится ли этот file_id внутри thumbnail
                                    if (thumbnailStart == -1 || fileIdStart < thumbnailStart || 
                                        fileIdStart > targetSticker.indexOf("}", thumbnailStart)) {
                                        // Это основной file_id, не thumbnail
                                        int fileIdEnd = targetSticker.indexOf("\"", fileIdStart + 11);
                                        if (fileIdEnd != -1) {
                                            String fileId = targetSticker.substring(fileIdStart + 11, fileIdEnd);
                                            LOGGER.info("✅ Найден основной file_id стикера: {}", fileId);
                                            return fileId;
                                        }
                                    }
                                    // Ищем следующий file_id
                                    fileIdStart = targetSticker.indexOf("\"file_id\":\"", fileIdStart + 1);
                                }
                            }
                            
                            LOGGER.warn("⚠️ Стикер с индексом {} не найден в ответе", stickerIndex);
                        }
                    }
                }
            }
            return null;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении file_id стикера: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Проверяет информацию о боте
     */
    public boolean checkBotInfo() {
        try {
            String url = apiUrl("getMe");
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && 
                response.getBody() != null && 
                response.getBody().contains("\"ok\":true")) {
                LOGGER.info("✅ Бот успешно прошел проверку getMe");
                return true;
            }
            
            LOGGER.error("❌ Неуспешная проверка бота: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody());
            return false;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при проверке информации о боте: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Формирует URL для API метода
     */
    private String apiUrl(String method) {
        return "https://api.telegram.org/bot" + botToken + "/" + method + "?";
    }
    
    /**
     * Информация о стикерпаке
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