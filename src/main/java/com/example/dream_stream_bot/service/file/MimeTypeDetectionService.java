package com.example.dream_stream_bot.service.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Сервис для определения MIME типов по file_id
 */
@Service
public class MimeTypeDetectionService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MimeTypeDetectionService.class);
    
    /**
     * Определяет MIME тип по Telegram file_id
     * 
     * @param fileId идентификатор файла из Telegram
     * @return MIME тип или application/octet-stream как fallback
     */
    public String detectMimeType(String fileId) {
        if (fileId == null || fileId.trim().isEmpty()) {
            LOGGER.warn("⚠️ Пустой file_id для определения MIME типа");
            return "application/octet-stream";
        }
        
        String mimeType = detectByPrefix(fileId);
        LOGGER.debug("🔍 Определен MIME тип для '{}': {}", fileId, mimeType);
        return mimeType;
    }
    
    /**
     * Определяет MIME тип по расширению файла из file_path
     */
    public String detectMimeTypeByExtension(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "application/octet-stream";
        }
        
        String extension = getFileExtension(filePath).toLowerCase();
        
        return switch (extension) {
            case "webp" -> "image/webp";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mp3" -> "audio/mpeg";
            case "ogg" -> "audio/ogg";
            case "tgs" -> "application/x-tgsticker"; // Telegram анимированные стикеры
            default -> "application/octet-stream";
        };
    }
    
    /**
     * Определяет MIME тип по префиксу file_id
     */
    private String detectByPrefix(String fileId) {
        // Анализ префиксов Telegram file_id
        if (fileId.startsWith("CAAC")) {
            return "image/webp"; // Стикеры обычно в WebP
        }
        if (fileId.startsWith("AQAD") || fileId.startsWith("AgAD")) {
            return "image/jpeg"; // Фотографии
        }
        if (fileId.startsWith("BAAC") || fileId.startsWith("BAADBAADBAAd")) {
            return "video/mp4"; // Видео
        }
        if (fileId.startsWith("DQAC") || fileId.startsWith("DQADBAADBAAd")) {
            return "application/pdf"; // Документы
        }
        if (fileId.startsWith("CQAC")) {
            return "audio/mpeg"; // Аудио
        }
        if (fileId.startsWith("CAIC")) {
            return "application/x-tgsticker"; // Анимированные стикеры (.tgs)
        }
        
        // Fallback
        return "application/octet-stream";
    }
    
    /**
     * Извлекает расширение файла из пути
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * Определяет имя файла по file_id и MIME типу
     */
    public String generateFileName(String fileId, String mimeType) {
        String extension = getExtensionFromMimeType(mimeType);
        return String.format("sticker_%s.%s", fileId.substring(0, Math.min(12, fileId.length())), extension);
    }
    
    /**
     * Получает расширение файла по MIME типу
     */
    private String getExtensionFromMimeType(String mimeType) {
        return switch (mimeType) {
            case "image/webp" -> "webp";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            case "audio/mpeg" -> "mp3";
            case "audio/ogg" -> "ogg";
            case "application/x-tgsticker" -> "tgs";
            default -> "bin";
        };
    }
    
    /**
     * Проверяет, является ли MIME тип допустимым для стикеров
     */
    public boolean isValidStickerMimeType(String mimeType) {
        return mimeType != null && (
                mimeType.equals("image/webp") ||
                mimeType.equals("image/png") ||
                mimeType.equals("image/gif") ||
                mimeType.equals("application/x-tgsticker")
        );
    }
}
