package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.StickerCacheDto;
import com.example.sticker_art_gallery.service.file.StickerCacheService;
import com.example.sticker_art_gallery.service.file.TelegramFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Контроллер для работы с файлами стикеров
 */
@RestController
@RequestMapping("/stickers")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET}) // CORS для файлов
@Tag(name = "Файлы стикеров", description = "Загрузка и кэширование файлов стикеров из Telegram")
public class StickerFileController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerFileController.class);
    
    private final StickerCacheService cacheService;
    private final TelegramFileService telegramFileService;
    
    @Autowired
    public StickerFileController(StickerCacheService cacheService, 
                                TelegramFileService telegramFileService) {
        this.cacheService = cacheService;
        this.telegramFileService = telegramFileService;
    }
    
    /**
     * Получить файл стикера по file_id
     */
    @GetMapping("/{fileId}")
    @Operation(
        summary = "Получить файл стикера",
        description = "Возвращает файл стикера по его Telegram file_id. " +
                     "Файл кэшируется в Redis на 7 дней для быстрого доступа. " +
                     "При первом запросе файл скачивается из Telegram Bot API."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Файл стикера успешно получен",
            content = @Content(
                mediaType = "image/webp",
                schema = @Schema(type = "string", format = "binary")
            )),
        @ApiResponse(responseCode = "400", description = "Некорректный file_id"),
        @ApiResponse(responseCode = "404", description = "Файл не найден в Telegram"),
        @ApiResponse(responseCode = "413", description = "Файл слишком большой (>512KB)"),
        @ApiResponse(responseCode = "429", description = "Превышен лимит запросов к Telegram API"),
        @ApiResponse(responseCode = "500", description = "Ошибка сервера или недоступность Redis/Telegram API")
    })
    public ResponseEntity<byte[]> getStickerFile(
            @Parameter(description = "Telegram file_id стикера", required = true, 
                      example = "CAACAgIAAxUAAWjHyNMRV143tmm6eW9_cqvr55qDAAIrXAACwQvZSjeyL1mWGuz7NgQ")
            @PathVariable 
            @NotBlank(message = "file_id не может быть пустым")
            @Pattern(regexp = "^[A-Za-z0-9_-]{10,100}$", 
                    message = "file_id должен содержать только буквы, цифры, _ и - (10-100 символов)")
            String fileId,
            
            @Parameter(description = "Имя бота для получения файла", example = "StickerGallery")
            @RequestParam(defaultValue = "StickerGallery") String botName) {
        
        try {
            LOGGER.info("📁 Запрос файла стикера: fileId={}, botName={}", fileId, botName);
            
            // 1. Валидация file_id
            if (!telegramFileService.isValidFileId(fileId)) {
                LOGGER.warn("⚠️ Некорректный file_id: {}", fileId);
                return ResponseEntity.badRequest().build();
            }
            
            // 2. Проверяем кэш Redis (если доступен)
            StickerCacheDto cached = null;
            if (cacheService.isRedisAvailable()) {
                cached = cacheService.get(fileId);
                if (cached != null) {
                    LOGGER.debug("🎯 Файл найден в кэше: {} байт", cached.getFileSize());
                    return buildFileResponse(cached, true);
                }
            } else {
                LOGGER.warn("⚠️ Redis недоступен, работаем без кэширования");
            }
            
            // 3. Скачиваем из Telegram API
            LOGGER.debug("📥 Файл не найден в кэше, скачиваем из Telegram...");
            StickerCacheDto downloaded = telegramFileService.downloadFile(fileId, botName);
            
            // 4. Сохраняем в кэш (если Redis доступен)
            if (cacheService.isRedisAvailable()) {
                cacheService.put(downloaded);
            }
            
            // 5. Возвращаем файл
            LOGGER.info("✅ Файл стикера успешно получен: {} байт, MIME: {}", 
                       downloaded.getFileSize(), downloaded.getMimeType());
            
            return buildFileResponse(downloaded, false);
            
        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Некорректный запрос для файла '{}': {}", fileId, e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Обработка специфичных ошибок Telegram API
            if (message.contains("file not found") || message.contains("FILE_ID_INVALID")) {
                LOGGER.warn("📂 Файл '{}' не найден в Telegram", fileId);
                return ResponseEntity.notFound().build();
            }
            
            if (message.contains("слишком большой")) {
                LOGGER.warn("📏 Файл '{}' слишком большой", fileId);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }
            
            if (message.contains("Too Many Requests") || message.contains("429")) {
                LOGGER.warn("🚫 Превышен лимит запросов к Telegram API для файла '{}'", fileId);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
            
            LOGGER.error("❌ Ошибка при получении файла '{}': {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            
        } catch (Exception e) {
            LOGGER.error("❌ Неожиданная ошибка при получении файла '{}': {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить статистику кэша стикеров
     */
    @GetMapping("/cache/stats")
    @Operation(
        summary = "Статистика кэша стикеров",
        description = "Возвращает информацию о состоянии кэша стикеров в Redis"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Статистика получена"),
        @ApiResponse(responseCode = "500", description = "Ошибка получения статистики")
    })
    public ResponseEntity<CacheStatsResponse> getCacheStats() {
        try {
            long cacheSize = cacheService.getCacheSize();
            boolean redisAvailable = cacheService.isRedisAvailable();
            
            CacheStatsResponse stats = new CacheStatsResponse(cacheSize, redisAvailable);
            
            LOGGER.debug("📊 Статистика кэша: размер={}, Redis доступен={}", cacheSize, redisAvailable);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении статистики кэша: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Создает HTTP ответ с файлом стикера
     */
    private ResponseEntity<byte[]> buildFileResponse(StickerCacheDto stickerCache, boolean fromCache) {
        byte[] fileData = stickerCache.getFileBytes();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*") // CORS
                .header("X-Cache-Status", fromCache ? "HIT" : "MISS") // Отладочная информация
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic()) // Кэш в браузере
                .contentType(MediaType.parseMediaType(stickerCache.getMimeType()))
                .contentLength(fileData.length)
                .body(fileData);
    }
    
    /**
     * DTO для статистики кэша
     */
    public record CacheStatsResponse(
            @Schema(description = "Количество файлов в кэше", example = "156") 
            long cachedFilesCount,
            
            @Schema(description = "Доступность Redis", example = "true") 
            boolean redisAvailable
    ) {}
}
