package com.example.sticker_art_gallery.service.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для проксирования запросов к внешнему сервису стикеров
 */
@Service
public class StickerProxyService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerProxyService.class);
    
    @Value("${STICKER_PROCESSOR_URL}")
    private String stickerProcessorUrl;
    
    private final RestTemplate restTemplate;
    
    @Autowired
    public StickerProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Получает комбинированное изображение всех стикеров стикерсета
     * @param name имя стикерсета
     * @param imageType тип изображения (thumbnail, full)
     * @param tileSize размер тайла в пикселях
     * @param maxStickers максимальное количество стикеров для объединения
     * @return массив байтов изображения в формате webp
     * @throws RuntimeException если произошла ошибка при запросе
     */
    public byte[] getCombinedStickerSetImage(String name, String imageType, int tileSize, int maxStickers) {
        LOGGER.info("🖼️ Получение комбинированного изображения стикерсета: name={}, imageType={}, tileSize={}, maxStickers={}", 
                   name, imageType, tileSize, maxStickers);
        
        try {
            String url = stickerProcessorUrl + "/stickers/combine-from-set";
            LOGGER.debug("🌐 Запрос к sticker-processor: {}", url);
            
            // Формируем тело запроса
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", name);
            requestBody.put("image_type", imageType);
            requestBody.put("tile_size", tileSize);
            requestBody.put("max_stickers", maxStickers);
            
            // Устанавливаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, request, byte[].class);
            long duration = System.currentTimeMillis() - startTime;
            
            byte[] imageData = response.getBody();
            String contentType = response.getHeaders().getFirst("Content-Type");
            
            LOGGER.info("✅ Комбинированное изображение получено: name={}, status={}, size={} bytes, duration={} ms, contentType={}", 
                       name, response.getStatusCode(), imageData != null ? imageData.length : 0, duration, contentType);
            
            if (imageData == null || imageData.length == 0) {
                throw new RuntimeException("Получено пустое изображение от sticker-processor");
            }
            
            return imageData;
            
        } catch (HttpClientErrorException e) {
            LOGGER.error("❌ Клиентская ошибка при получении комбинированного изображения '{}': {} {}", 
                        name, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Ошибка при запросе комбинированного изображения: " + e.getStatusCode(), e);
            
        } catch (HttpServerErrorException e) {
            LOGGER.error("❌ Серверная ошибка при получении комбинированного изображения '{}': {} {}", 
                        name, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Ошибка сервера sticker-processor: " + e.getStatusCode(), e);
            
        } catch (ResourceAccessException e) {
            LOGGER.error("❌ Ошибка подключения при получении комбинированного изображения '{}': {}", name, e.getMessage());
            throw new RuntimeException("Не удалось подключиться к sticker-processor", e);
            
        } catch (Exception e) {
            LOGGER.error("❌ Неожиданная ошибка при получении комбинированного изображения '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении комбинированного изображения: " + e.getMessage(), e);
        }
    }
}
