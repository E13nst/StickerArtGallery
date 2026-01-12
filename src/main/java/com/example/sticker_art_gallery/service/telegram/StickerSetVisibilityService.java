package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.repository.ArtTransactionRepository;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления видимостью и статусом стикерсетов
 */
@Service
public class StickerSetVisibilityService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetVisibilityService.class);
    private final StickerSetRepository stickerSetRepository;
    private final ArtRewardService artRewardService;
    private final ArtTransactionRepository artTransactionRepository;
    
    @Autowired
    public StickerSetVisibilityService(StickerSetRepository stickerSetRepository,
                                      ArtRewardService artRewardService,
                                      ArtTransactionRepository artTransactionRepository) {
        this.stickerSetRepository = stickerSetRepository;
        this.artRewardService = artRewardService;
        this.artTransactionRepository = artTransactionRepository;
    }
    
    /**
     * Опубликовать стикерсет (PRIVATE -> PUBLIC) с начислением ART за первую публикацию
     */
    @Transactional
    public StickerSet publishStickerSet(Long id) {
        StickerSet stickerSet = stickerSetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет не найден"));
        
        // Проверяем, не публичный ли уже
        if (stickerSet.isPublic()) {
            LOGGER.debug("Стикерсет ID={} уже публичный", id);
            return stickerSet; // Уже публичный, ничего не делаем
        }
        
        // Меняем видимость
        stickerSet.setVisibility(StickerSetVisibility.PUBLIC);
        StickerSet saved = stickerSetRepository.save(stickerSet);
        
        // Начисляем ART за ПЕРВУЮ публикацию этого name
        String stickerName = stickerSet.getName();
        if (!hasAnyArtTransactionForName(stickerName)) {
            try {
                String metadata = String.format("{\"stickerSetId\":%d,\"name\":\"%s\"}", 
                                              id, stickerName);
                String externalId = "sticker-publish:" + stickerName; // по name!
                artRewardService.award(
                    stickerSet.getUserId(),
                    ArtRewardService.RULE_PUBLISH_STICKERSET,
                    null,
                    metadata,
                    externalId,
                    stickerSet.getUserId()
                );
                LOGGER.info("💎 Начислено 10 ART за публикацию стикерсета: name={}, userId={}", stickerName, stickerSet.getUserId());
            } catch (Exception e) {
                LOGGER.warn("⚠️ Не удалось начислить ART за публикацию: {}", e.getMessage());
            }
        } else {
            LOGGER.info("♻️ ART уже начислялись за стикерсет с name={}, пропускаем", stickerName);
        }
        
        return saved;
    }
    
    /**
     * Сделать стикерсет приватным (PUBLIC -> PRIVATE)
     */
    @Transactional
    public StickerSet unpublishStickerSet(Long id) {
        StickerSet stickerSet = stickerSetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет не найден"));
        
        // Проверяем, не приватный ли уже
        if (stickerSet.isPrivate()) {
            LOGGER.debug("Стикерсет ID={} уже приватный", id);
            return stickerSet; // Уже приватный, ничего не делаем
        }
        
        // Меняем видимость
        stickerSet.setVisibility(StickerSetVisibility.PRIVATE);
        StickerSet saved = stickerSetRepository.save(stickerSet);
        LOGGER.info("✅ Стикерсет ID={} сделан приватным", id);
        
        return saved;
    }
    
    /**
     * Проверяет, есть ли транзакции ART для стикерсета с указанным name
     */
    private boolean hasAnyArtTransactionForName(String name) {
        return artTransactionRepository.existsByNameInMetadata(name);
    }
    
    /**
     * Заблокировать стикерсет (только для админа)
     */
    @Transactional
    public StickerSet blockStickerSet(Long stickerSetId, String reason) {
        LOGGER.info("🚫 Блокировка стикерсета ID: {}, причина: {}", stickerSetId, reason);
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        stickerSet.markAsBlocked(reason); // state -> BLOCKED, blockReason -> reason
        
        StickerSet savedStickerSet = stickerSetRepository.save(stickerSet);
        LOGGER.info("✅ Стикерсет {} успешно заблокирован", stickerSetId);
        
        return savedStickerSet;
    }
    
    /**
     * Разблокировать стикерсет (только для админа)
     */
    @Transactional
    public StickerSet unblockStickerSet(Long stickerSetId) {
        LOGGER.info("✅ Разблокировка стикерсета ID: {}", stickerSetId);
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        if (stickerSet.isBlocked()) {
            stickerSet.setState(StickerSetState.ACTIVE);
            stickerSet.setBlockReason(null);
            
            StickerSet savedStickerSet = stickerSetRepository.save(stickerSet);
            LOGGER.info("✅ Стикерсет {} успешно разблокирован", stickerSetId);
            
            return savedStickerSet;
        }
        
        return stickerSet;
    }
    
    /**
     * Установить официальный статус стикерсета (только для админа)
     */
    @Transactional
    public StickerSet setOfficial(Long stickerSetId) {
        LOGGER.info("🏅 Установка статуса ОФИЦИАЛЬНЫЙ для стикерсета ID: {}", stickerSetId);
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        stickerSet.setType(StickerSetType.OFFICIAL);
        StickerSet saved = stickerSetRepository.save(stickerSet);
        LOGGER.info("✅ Стикерсет {} отмечен как официальный", stickerSetId);
        return saved;
    }
    
    /**
     * Снять официальный статус стикерсета (только для админа)
     */
    @Transactional
    public StickerSet unsetOfficial(Long stickerSetId) {
        LOGGER.info("🏷️ Снятие статуса ОФИЦИАЛЬНЫЙ для стикерсета ID: {}", stickerSetId);
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        stickerSet.setType(StickerSetType.USER);
        StickerSet saved = stickerSetRepository.save(stickerSet);
        LOGGER.info("✅ Стикерсет {} отмечен как неофициальный", stickerSetId);
        return saved;
    }
    
    /**
     * Установить автора стикерсета (только для админа)
     */
    @Transactional
    public StickerSet setAuthor(Long stickerSetId, Long authorId) {
        if (authorId == null || authorId <= 0) {
            throw new IllegalArgumentException("authorId должен быть положительным числом");
        }
        LOGGER.info("✍️ Установка автора {} для стикерсета {}", authorId, stickerSetId);
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        stickerSet.setAuthorId(authorId);
        return stickerSetRepository.save(stickerSet);
    }
    
    /**
     * Очистить автора стикерсета (только для админа)
     */
    @Transactional
    public StickerSet clearAuthor(Long stickerSetId) {
        LOGGER.info("🧹 Очистка автора для стикерсета {}", stickerSetId);
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        stickerSet.setAuthorId(null);
        return stickerSetRepository.save(stickerSet);
    }
}
