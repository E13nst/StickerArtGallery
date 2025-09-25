package com.example.sticker_art_gallery.service.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.File;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сервис для скачивания файлов из Telegram
 */
@Service
public class FileDownloadService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadService.class);
    
    /**
     * Скачивает файл из Telegram во временную директорию
     * @param file информация о файле из Telegram
     * @param botToken токен бота
     * @return путь к скачанному файлу
     */
    public Path downloadTelegramFile(File file, String botToken) throws Exception {
        String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
        LOGGER.info("📥 Скачиваем файл: {}", fileUrl);
        
        // Создаем временный файл
        String extension = getFileExtension(file.getFilePath());
        Path tempFile = Files.createTempFile("telegram_image_", "." + extension);
        
        // Скачиваем файл
        try (InputStream inputStream = new URL(fileUrl).openStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
            
            inputStream.transferTo(outputStream);
        }
        
        LOGGER.info("📥 Файл скачан: {} | Размер: {} bytes", tempFile, Files.size(tempFile));
        
        return tempFile;
    }
    
    /**
     * Получает расширение файла из пути
     */
    private String getFileExtension(String filePath) {
        if (filePath == null) return "jpg";
        
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "jpg"; // по умолчанию
    }
    
    /**
     * Удаляет временный файл
     */
    public void cleanupTempFile(Path tempFile) {
        try {
            if (tempFile != null && Files.exists(tempFile)) {
                Files.delete(tempFile);
                LOGGER.debug("🗑️ Временный файл удален: {}", tempFile);
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось удалить временный файл {}: {}", tempFile, e.getMessage());
        }
    }
}