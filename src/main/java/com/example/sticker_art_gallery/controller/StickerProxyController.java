package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.service.proxy.StickerProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Прокси-контроллер для работы с внешним сервисом стикеров
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET})
@Tag(name = "Прокси стикеров", description = "Проксирование запросов к внешнему сервису стикеров")
public class StickerProxyController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerProxyController.class);
    
    private final StickerProxyService stickerProxyService;
    
    @Autowired
    public StickerProxyController(StickerProxyService stickerProxyService) {
        this.stickerProxyService = stickerProxyService;
    }
    
    /**
     * Получить файл стикера по file_id (проксирование)
     */
    @GetMapping("/stickers/{fileId}")
    @Operation(
        summary = "Получить файл стикера",
        description = "Проксирует запрос к внешнему сервису стикеров с кэшированием в Redis"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Файл стикера успешно получен"),
        @ApiResponse(responseCode = "400", description = "Некорректный file_id"),
        @ApiResponse(responseCode = "404", description = "Стикер не найден"),
        @ApiResponse(responseCode = "502", description = "Ошибка внешнего сервиса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Object> getSticker(
            @Parameter(description = "Telegram file_id стикера", required = true, 
                      example = "CAACAgIAAxUAAWjHy88gzacLGK1i0RSiNtiW81kJAALgAAP3AsgPYqAgfkyPleo2BA")
            @PathVariable 
            @NotBlank(message = "file_id не может быть пустым")
            @Pattern(regexp = "^[A-Za-z0-9_-]{10,100}$", 
                    message = "file_id должен содержать только буквы, цифры, _ и - (10-100 символов)")
            String fileId) {
        
        try {
            LOGGER.info("📁 Прокси-запрос файла стикера: fileId={}", fileId);
            
            // Валидация file_id
            if (!isValidFileId(fileId)) {
                LOGGER.warn("⚠️ Некорректный file_id: {}", fileId);
                return ResponseEntity.badRequest().build();
            }
            
            // Получаем стикер через прокси-сервис (с кэшированием)
            ResponseEntity<Object> response = stickerProxyService.getSticker(fileId);
            
            LOGGER.info("✅ Прокси-запрос выполнен успешно: fileId={}, status={}", 
                       fileId, response.getStatusCode());
            
            return response;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при проксировании запроса стикера {}: {}", fileId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить статистику кэша (проксирование)
     */
    @GetMapping("/stickers/cache/stats")
    @Operation(
        summary = "Получить статистику кэша",
        description = "Проксирует запрос статистики кэша к внешнему сервису стикеров"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статистика кэша успешно получена"),
        @ApiResponse(responseCode = "502", description = "Ошибка внешнего сервиса"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Object> getCacheStats() {
        try {
            LOGGER.info("📊 Прокси-запрос статистики кэша");
            
            ResponseEntity<Object> response = stickerProxyService.getCacheStats();
            
            LOGGER.info("✅ Прокси-запрос статистики выполнен успешно: status={}", 
                       response.getStatusCode());
            
            return response;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при проксировании запроса статистики: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Простая валидация file_id
     */
    private boolean isValidFileId(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            return false;
        }
        
        // Проверяем длину и символы
        return fileId.length() >= 10 && fileId.length() <= 100 && 
               fileId.matches("^[A-Za-z0-9_-]+$");
    }
}
