package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.StickerSet;
import com.example.dream_stream_bot.service.ai.AIService;
import com.example.dream_stream_bot.service.file.FileDownloadService;
import com.example.dream_stream_bot.service.image.ImageOptimizationService;
import com.example.dream_stream_bot.service.telegram.TelegramApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Service
public class StickerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerService.class);
    
    private final AIService aiService;
    private final FileDownloadService fileDownloadService;
    private final ImageOptimizationService imageOptimizationService;
    private final TelegramApiService telegramApiService;
    private StickerSetService stickerSetService;
    
    @Autowired
    public StickerService(AIService aiService, FileDownloadService fileDownloadService,
                         ImageOptimizationService imageOptimizationService, TelegramApiService telegramApiService) {
        this.aiService = aiService;
        this.fileDownloadService = fileDownloadService;
        this.imageOptimizationService = imageOptimizationService;
        this.telegramApiService = telegramApiService;
    }
    
    @Autowired
    public void setStickerSetService(StickerSetService stickerSetService) {
        this.stickerSetService = stickerSetService;
    }
    
    /**
     * Обрабатывает сообщение с изображением и создает стикер
     */
    public SendMessage handleImageMessage(Message message, TelegramLongPollingBot bot, String stickerPackName) {
        LOGGER.info("📨 Получено сообщение | Тип: {} | ChatId: {} | StickerPackName: {}",
                message.getMediaGroupId(), message.getChatId(), stickerPackName);
        
        // Устанавливаем токен бота для TelegramApiService
        telegramApiService.setBotToken(bot.getBotToken());
        LOGGER.info("🔑 Установлен токен бота для TelegramApiService");
        
        try {
            if (message.hasPhoto()) {
                return processPhoto(message, bot, stickerPackName);
            } else if (message.hasDocument()) {
                return processDocument(message, bot, stickerPackName);
            } else {
                return SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("❌ Неподдерживаемый тип сообщения. Отправьте изображение (фото или документ).")
                        .build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обработке файла фото: {}", e.getMessage(), e);
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Произошла ошибка при обработке изображения. Попробуйте еще раз.")
                    .build();
        }
    }
    
    /**
     * Обрабатывает фото сообщение
     */
    private SendMessage processPhoto(Message message, TelegramLongPollingBot bot, String stickerPackName) throws Exception {
        LOGGER.info("📸 Обрабатываем фото сообщение");
        
        // Получаем файл с максимальным размером
        java.util.List<org.telegram.telegrambots.meta.api.objects.PhotoSize> photoSizes = message.getPhoto();
        org.telegram.telegrambots.meta.api.objects.PhotoSize largestPhoto = photoSizes.get(photoSizes.size() - 1);
        
        String fileId = largestPhoto.getFileId();
        LOGGER.info("📁 FileId фото: {}", fileId);
        
        // Получаем информацию о файле
        org.telegram.telegrambots.meta.api.methods.GetFile getFile = new org.telegram.telegrambots.meta.api.methods.GetFile();
        getFile.setFileId(fileId);
        org.telegram.telegrambots.meta.api.objects.File file = bot.execute(getFile);
        
        if (file == null || file.getFilePath() == null) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Не удалось получить файл изображения.")
                    .build();
        }
        
        // Скачиваем файл
        Path downloadedFile = fileDownloadService.downloadTelegramFile(file, bot.getBotToken());
        if (downloadedFile == null) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Не удалось скачать файл. Попробуйте еще раз.")
                    .build();
        }
        
        return processImageFile(downloadedFile.toFile(), message.getChatId(), bot, "фото", stickerPackName);
    }
    
    /**
     * Обрабатывает документ сообщение
     */
    private SendMessage processDocument(Message message, TelegramLongPollingBot bot, String stickerPackName) throws Exception {
        LOGGER.info("📄 Обрабатываем документ сообщение");
        
        org.telegram.telegrambots.meta.api.objects.Document document = message.getDocument();
        String mimeType = document.getMimeType();
        
        // Проверяем, что это изображение
        if (mimeType == null || !mimeType.startsWith("image/")) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Неподдерживаемый тип файла. Отправьте изображение (JPG, PNG, GIF).")
                    .build();
        }
        
        String fileId = document.getFileId();
        LOGGER.info("📁 FileId документа: {}", fileId);
        
        // Получаем информацию о файле
        org.telegram.telegrambots.meta.api.methods.GetFile getFile = new org.telegram.telegrambots.meta.api.methods.GetFile();
        getFile.setFileId(fileId);
        org.telegram.telegrambots.meta.api.objects.File file = bot.execute(getFile);
        
        if (file == null || file.getFilePath() == null) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Не удалось получить файл изображения.")
                    .build();
        }
        
        // Скачиваем файл
        Path downloadedFile = fileDownloadService.downloadTelegramFile(file, bot.getBotToken());
        if (downloadedFile == null) {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("❌ Не удалось скачать файл. Попробуйте еще раз.")
                    .build();
        }
        
        return processImageFile(downloadedFile.toFile(), message.getChatId(), bot, "документ", stickerPackName);
    }
    
    /**
     * Обрабатывает файл изображения
     */
    private SendMessage processImageFile(File file, Long chatId, TelegramLongPollingBot bot, String fileType, String stickerPackName) {
        try {
            LOGGER.info("🔧 Обрабатываем файл {}: {}", fileType, file.getName());
            
            // Оптимизируем изображение для стикера
            Path optimizedFile = imageOptimizationService.optimizeImageForSticker(file.toPath());
            LOGGER.info("✅ Изображение оптимизировано: {}", optimizedFile.getFileName());
            
            // Создаем или добавляем к стикерпаку
            StickerResult result = createOrAddToStickerPack(chatId, optimizedFile, stickerPackName);
            
            if (result.isSuccess()) {
                // Отправляем оптимизированное изображение с информацией
                sendOptimizedImage(chatId, bot, optimizedFile, result);
                return null; // Сообщение уже отправлено
            } else {
                // Удаляем временный файл
                Files.deleteIfExists(optimizedFile);
                
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("❌ Не удалось создать стикер: " + result.getErrorMessage())
                        .build();
            }
                    
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обработке файла {}: {}", fileType, e.getMessage(), e);
            return SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ Произошла ошибка при обработке изображения. Попробуйте еще раз.")
                    .build();
        }
    }
    
    /**
     * Создает новый стикерпак или добавляет к существующему
     */
    private StickerResult createOrAddToStickerPack(Long chatId, Path optimizedFile, String stickerPackName) {
        try {
            if (stickerPackName == null || stickerPackName.isEmpty()) {
                return StickerResult.failure("Не указано имя стикерпака");
            }
            
            // Ищем стикерпак в базе данных
            StickerSet stickerSet = stickerSetService.findByName(stickerPackName);
            if (stickerSet == null) {
                return StickerResult.failure("Стикерпак не найден в базе данных: " + stickerPackName);
            }
            
            // Проверяем, существует ли стикерпак в Telegram
            TelegramApiService.StickerSetInfo telegramInfo = telegramApiService.getStickerSetInfo(stickerPackName);
            
            if (telegramInfo != null && telegramInfo.exists()) {
                // Стикерпак существует, добавляем к нему
                LOGGER.info("➕ Добавляем стикер к существующему стикерпаку: {}", stickerPackName);
                boolean added = telegramApiService.addStickerToSet(chatId, optimizedFile.toFile(), stickerPackName, "🎯");
                
                if (added) {
                    return StickerResult.success(stickerPackName, stickerSet.getTitle(), false, telegramInfo.getStickerCount());
                } else {
                    return StickerResult.failure("Не удалось добавить стикер к существующему стикерпаку");
                }
            } else {
                // Стикерпак не существует в Telegram, создаем новый
                LOGGER.info("📦 Создаем новый стикерпак в Telegram: {}", stickerPackName);
                boolean created = telegramApiService.createNewStickerSet(chatId, optimizedFile.toFile(), stickerPackName, stickerSet.getTitle(), "🎯");
                
                if (created) {
                    return StickerResult.success(stickerPackName, stickerSet.getTitle(), true, 0);
                } else {
                    return StickerResult.failure("Не удалось создать новый стикерпак в Telegram");
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании/добавлении стикера: {}", e.getMessage(), e);
            return StickerResult.failure("Ошибка при создании стикера: " + e.getMessage());
        }
    }
    
    /**
     * Отправляет оптимизированное изображение с информацией о стикере
     */
    private void sendOptimizedImage(Long chatId, TelegramLongPollingBot bot, Path optimizedFile, StickerResult result) {
        try {
            String caption = result != null && result.isSuccess() ?
                "🎉 Стикер создан успешно!\n\n" +
                "📦 Информация о стикерпаке:\n" +
                "• Ссылка: https://t.me/addstickers/" + result.getStickerSetName() + "\n" +
                "• Заголовок: " + result.getStickerSetTitle() + "\n" +
                "• Тип: " + (result.isNewSet() ? "Новый стикерпак" : "Добавлен к существующему") :
                "📸 Изображение оптимизировано для стикера";

            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(optimizedFile.toFile()))
                    .caption(caption)
                    .build();

            LOGGER.info("🚀 Отправляем изображение в чат: {}", chatId);
            bot.execute(sendPhoto);
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при отправке изображения: {}", e.getMessage());
        }
    }

    /**
     * Результат операции со стикером
     */
    public static class StickerResult {
        private final boolean success;
        private final String stickerSetName;
        private final String stickerSetTitle;
        private final boolean isNewSet;
        private final int stickerCount;
        private final String errorMessage;
        
        private StickerResult(boolean success, String stickerSetName, String stickerSetTitle, 
                            boolean isNewSet, int stickerCount, String errorMessage) {
            this.success = success;
            this.stickerSetName = stickerSetName;
            this.stickerSetTitle = stickerSetTitle;
            this.isNewSet = isNewSet;
            this.stickerCount = stickerCount;
            this.errorMessage = errorMessage;
        }
        
        public static StickerResult success(String stickerSetName, String stickerSetTitle, boolean isNewSet, int stickerCount) {
            return new StickerResult(true, stickerSetName, stickerSetTitle, isNewSet, stickerCount, null);
        }
        
        public static StickerResult failure(String errorMessage) {
            return new StickerResult(false, null, null, false, 0, errorMessage);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getStickerSetName() { return stickerSetName; }
        public String getStickerSetTitle() { return stickerSetTitle; }
        public boolean isNewSet() { return isNewSet; }
        public int getStickerCount() { return stickerCount; }
        public String getErrorMessage() { return errorMessage; }
    }
} 