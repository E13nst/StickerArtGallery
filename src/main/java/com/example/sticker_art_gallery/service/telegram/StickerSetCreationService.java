package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.dto.SaveImageToStickerSetResponseDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Set;

/**
 * Сервис для создания и управления стикерсетами через Telegram Bot API.
 * Упрощенная архитектура с двумя главными методами.
 */
@Service
public class StickerSetCreationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetCreationService.class);
    private static final int MAX_STICKERS_PER_SET = 120;
    private static final String DEFAULT_EMOJI = "🎨";
    
    private final ImageStorageService imageStorageService;
    private final TelegramBotApiService telegramBotApiService;
    private final StickerSetService stickerSetService;
    private final StickerSetNamingService namingService;
    private final StickerSetTelegramCacheService stickerSetTelegramCacheService;
    private final UserRepository userRepository;
    private final AppConfig appConfig;
    
    @Autowired
    public StickerSetCreationService(
            ImageStorageService imageStorageService,
            TelegramBotApiService telegramBotApiService,
            StickerSetService stickerSetService,
            StickerSetNamingService namingService,
            StickerSetTelegramCacheService stickerSetTelegramCacheService,
            UserRepository userRepository,
            AppConfig appConfig) {
        this.imageStorageService = imageStorageService;
        this.telegramBotApiService = telegramBotApiService;
        this.stickerSetService = stickerSetService;
        this.namingService = namingService;
        this.stickerSetTelegramCacheService = stickerSetTelegramCacheService;
        this.userRepository = userRepository;
        this.appConfig = appConfig;
    }
    
    /**
     * Создает новый стикерсет в Telegram с первым стикером и регистрирует его в БД.
     * 
     * @param userId ID пользователя
     * @param imageUuid UUID файла изображения в /data/images
     * @param title название стикерсета (опционально)
     * @param name имя стикерсета (опционально, автогенерация если не указан)
     * @param emoji эмодзи для стикера (опционально, по умолчанию "🎨")
     * @param categoryKeys ключи категорий (опционально)
     * @param visibility видимость стикерсета (опционально, по умолчанию PRIVATE)
     * @return созданный StickerSet или null если регистрация в БД не удалась
     */
    @Transactional
    public StickerSet createWithSticker(
            Long userId,
            java.util.UUID imageUuid,
            String title,
            String name,
            String emoji,
            Set<String> categoryKeys,
            StickerSetVisibility visibility) {
        
        LOGGER.info("🎯 Создание стикерсета с первым стикером: userId={}, imageUuid={}, name={}", 
                userId, imageUuid, name);
        
        // 1. Получить файл
        File stickerFile = imageStorageService.getFileByUuid(imageUuid);
        
        // 2. Генерировать имя если не указано, затем гарантировать суффикс _by_<bot>
        if (name == null || name.isBlank()) {
            String username = getUserUsername(userId);
            name = namingService.generateDefaultName(userId, username);
            LOGGER.debug("📝 Сгенерировано имя стикерсета: {}", name);
        }
        if (name != null && !name.isBlank()) {
            name = namingService.ensureBotSuffix(name);
        }

        // 3. Установить дефолтные значения
        if (title == null || title.isBlank()) {
            title = appConfig.getTelegram().getDefaultStickerSetTitle();
            if (title == null || title.isBlank()) {
                title = "Styxly Generated";
            }
        }
        
        if (emoji == null || emoji.isBlank()) {
            emoji = DEFAULT_EMOJI;
        }
        
        if (visibility == null) {
            visibility = StickerSetVisibility.PRIVATE;
        }
        
        // 4. Создать в Telegram
        boolean success = telegramBotApiService.createNewStickerSet(
            userId, stickerFile, name, title, emoji
        );
        
        if (!success) {
            throw new RuntimeException("Failed to create sticker set in Telegram: " + name);
        }
        
        LOGGER.info("✅ Стикерсет создан в Telegram: {}", name);
        
        // 5. Зарегистрировать в БД (простая стратегия: если упало - логируем)
        try {
            CreateStickerSetDto dto = new CreateStickerSetDto();
            dto.setName(name);
            dto.setTitle(title);
            dto.setCategoryKeys(categoryKeys);
            dto.setVisibility(visibility);
            
            StickerSet stickerSet = stickerSetService.createStickerSetForUser(dto, userId, "en", null);
            LOGGER.info("✅ Стикерсет зарегистрирован в БД: id={}, name={}", stickerSet.getId(), name);

            // Обновляем persistent cache сразу после успешного создания
            try {
                Object payload = telegramBotApiService.getStickerSetInfo(name);
                stickerSetTelegramCacheService.save(stickerSet.getId(), name, payload);
            } catch (Exception e) {
                LOGGER.warn("⚠️ Не удалось сохранить кеш Telegram payload для нового стикерсета {}: {}", name, e.getMessage());
            }
            return stickerSet;
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при регистрации стикерсета в БД: {}", e.getMessage(), e);
            // Не откатываем Telegram, просто логируем
            return null;
        }
    }
    
    /**
     * Универсальный метод: сохраняет изображение в стикерсет (или создает дефолтный если не указан).
     * 
     * @param userId ID пользователя
     * @param imageUuid UUID файла изображения в /data/images
     * @param stickerSetName имя стикерсета (опционально, дефолтный если не указан)
     * @param emoji эмодзи для стикера (опционально, по умолчанию "🎨")
     */
    @Transactional
    public SaveImageToStickerSetResponseDto saveImageToStickerSet(
            Long userId,
            java.util.UUID imageUuid,
            String stickerSetName,
            String emoji) {
        
        LOGGER.info("💾 Сохранение изображения в стикерсет: userId={}, imageUuid={}, stickerSetName={}", 
                userId, imageUuid, stickerSetName);
        
        // 1. Определить стикерсет
        if (stickerSetName == null || stickerSetName.isBlank()) {
            // Получить имя дефолтного стикерсета
            String username = getUserUsername(userId);
            stickerSetName = namingService.generateDefaultName(userId, username);
            
            // Проверить существование в Telegram
            TelegramBotApiService.StickerSetInfo setInfo = telegramBotApiService.getStickerSetInfoSimple(stickerSetName);
            if (setInfo == null || !setInfo.exists()) {
                // Дефолтный стикерсет не существует - создаем его с текущим стикером
                LOGGER.info("📦 Дефолтный стикерсет не существует, создаем: {}", stickerSetName);
                String defaultTitle = appConfig.getTelegram().getDefaultStickerSetTitle();
                if (defaultTitle == null || defaultTitle.isBlank()) {
                    defaultTitle = "Styxly Generated";
                }
                
                File stickerFile = imageStorageService.getFileByUuid(imageUuid);
                if (emoji == null || emoji.isBlank()) {
                    emoji = DEFAULT_EMOJI;
                }
                
                boolean success = telegramBotApiService.createNewStickerSet(
                    userId, stickerFile, stickerSetName, defaultTitle, emoji
                );
                
                if (!success) {
                    throw new RuntimeException("Failed to create default sticker set: " + stickerSetName);
                }
                
                LOGGER.info("✅ Дефолтный стикерсет создан: {}", stickerSetName);
                
                // Стикер уже добавлен при создании, индекс будет 0
                String stickerFileId = telegramBotApiService.getStickerFileId(stickerSetName, 0);
                if (stickerFileId == null || stickerFileId.isBlank()) {
                    throw new RuntimeException("Failed to resolve sticker file_id after creating set: " + stickerSetName);
                }
                
                return new SaveImageToStickerSetResponseDto(stickerSetName, 0, stickerFileId);
            }
        } else {
            // Валидация владения
            namingService.validateStickerSetName(stickerSetName);
        }
        
        // 2. Проверить лимит
        TelegramBotApiService.StickerSetInfo setInfo = telegramBotApiService.getStickerSetInfoSimple(stickerSetName);
        if (setInfo != null && setInfo.exists() && setInfo.getStickerCount() >= MAX_STICKERS_PER_SET) {
            throw new IllegalStateException(
                "Sticker set is full (120 stickers max). Please create a new set."
            );
        }
        
        // 2.1. Получить предыдущее состояние набора (file_id в порядке Telegram)
        java.util.List<String> previousFileIds = telegramBotApiService.getStickerFileIdsInOrder(stickerSetName);
        
        // 3. Получить файл и добавить
        File stickerFile = imageStorageService.getFileByUuid(imageUuid);
        
        if (emoji == null || emoji.isBlank()) {
            emoji = DEFAULT_EMOJI;
        }
        
        boolean success = telegramBotApiService.addStickerToSet(
            userId, stickerFile, stickerSetName, emoji
        );
        
        if (!success) {
            throw new RuntimeException("Failed to add sticker to set: " + stickerSetName);
        }
        
        LOGGER.info("✅ Стикер добавлен в стикерсет: {}", stickerSetName);

        // 4. Получить новое состояние набора и найти добавленный стикер по сравнению с предыдущим
        java.util.List<String> updatedFileIds = telegramBotApiService.getStickerFileIdsInOrder(stickerSetName);
        if (updatedFileIds == null || updatedFileIds.isEmpty()) {
            throw new RuntimeException("Failed to fetch updated sticker set file_ids: " + stickerSetName);
        }

        java.util.Set<String> previousSet = previousFileIds != null
            ? new java.util.HashSet<>(previousFileIds)
            : java.util.Collections.emptySet();

        String newStickerFileId = null;
        int stickerIndex = -1;
        for (int i = 0; i < updatedFileIds.size(); i++) {
            String fileId = updatedFileIds.get(i);
            if (!previousSet.contains(fileId)) {
                newStickerFileId = fileId;
                stickerIndex = i;
                break;
            }
        }

        // Фоллбек: если не нашли отличия, но размер увеличился на 1 — берём последний
        if (newStickerFileId == null && previousFileIds != null 
                && updatedFileIds.size() == previousFileIds.size() + 1) {
            stickerIndex = updatedFileIds.size() - 1;
            newStickerFileId = updatedFileIds.get(stickerIndex);
        }

        if (newStickerFileId == null || stickerIndex < 0) {
            throw new RuntimeException("Failed to determine new sticker file_id for set: " + stickerSetName);
        }

        // 5. Получить title стикерсета
        Object fullStickerSetInfo = telegramBotApiService.getStickerSetInfo(stickerSetName);
        String title = telegramBotApiService.extractTitleFromStickerSetInfo(fullStickerSetInfo);

        // Сохраняем обновленный payload в persistent cache, если стикерсет зарегистрирован в БД
        StickerSet existingStickerSet = stickerSetService.findByName(stickerSetName);
        if (existingStickerSet != null) {
            try {
                stickerSetTelegramCacheService.save(existingStickerSet.getId(), stickerSetName, fullStickerSetInfo);
            } catch (Exception e) {
                LOGGER.warn("⚠️ Не удалось сохранить кеш Telegram payload для стикерсета {}: {}", stickerSetName, e.getMessage());
            }
        }

        return new SaveImageToStickerSetResponseDto(stickerSetName, stickerIndex, newStickerFileId, title);
    }
    
    /**
     * Получить username пользователя.
     * 
     * @param userId ID пользователя
     * @return username или null если не найден
     */
    private String getUserUsername(Long userId) {
        return userRepository.findById(userId)
            .map(UserEntity::getUsername)
            .orElse(null);
    }
}
