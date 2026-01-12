package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.service.category.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Сервис для базовых CRUD операций со стикерсетами
 */
@Service
public class StickerSetCrudService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetCrudService.class);
    private final StickerSetRepository stickerSetRepository;
    private final CategoryService categoryService;
    
    @Autowired
    public StickerSetCrudService(StickerSetRepository stickerSetRepository,
                                CategoryService categoryService) {
        this.stickerSetRepository = stickerSetRepository;
        this.categoryService = categoryService;
    }
    
    /**
     * Найти стикерсет по ID
     */
    public StickerSet findById(Long id) {
        return stickerSetRepository.findById(id).orElse(null);
    }
    
    /**
     * Найти стикерсет по имени
     */
    public StickerSet findByName(String name) {
        return stickerSetRepository.findByName(name).orElse(null);
    }
    
    /**
     * Найти стикерсет по заголовку
     */
    public StickerSet findByTitle(String title) {
        return stickerSetRepository.findByTitle(title);
    }
    
    /**
     * Найти все стикерсеты пользователя
     */
    public List<StickerSet> findByUserId(Long userId) {
        return stickerSetRepository.findByUserId(userId);
    }
    
    /**
     * Найти все стикерсеты
     */
    public List<StickerSet> findAll() {
        return stickerSetRepository.findAll();
    }
    
    /**
     * Сохранить стикерсет
     */
    public StickerSet save(StickerSet stickerSet) {
        LOGGER.debug("Сохранение стикерсета для пользователя {}", stickerSet.getUserId());
        return stickerSetRepository.save(stickerSet);
    }
    
    /**
     * Удалить стикерсет (soft delete)
     */
    @Transactional
    public void deleteById(Long id) {
        StickerSet stickerSet = findById(id);
        if (stickerSet != null && stickerSet.isActive()) {
            stickerSet.markAsDeleted(); // state -> DELETED, deletedAt -> now
            stickerSetRepository.save(stickerSet);
            LOGGER.info("🗑️ Стикерсет ID={} помечен как DELETED", id);
        }
    }
    
    /**
     * Обновить категории стикерсета
     */
    @Transactional
    public StickerSet updateCategories(Long stickerSetId, Set<String> categoryKeys) {
        LOGGER.info("🏷️ Обновление категорий стикерсета ID: {}, категории: {}", stickerSetId, categoryKeys);
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        // Очищаем существующие категории
        stickerSet.clearCategories();
        
        // Добавляем новые категории, если они указаны
        if (categoryKeys != null && !categoryKeys.isEmpty()) {
            try {
                List<Category> categories = categoryService.getCategoriesByKeys(categoryKeys);
                for (Category category : categories) {
                    stickerSet.addCategory(category);
                }
                LOGGER.info("✅ Добавлено {} категорий к стикерсету {}", categories.size(), stickerSetId);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("⚠️ Ошибка при получении категорий: {}", e.getMessage());
                throw e;
            }
        }
        
        StickerSet savedStickerSet = stickerSetRepository.save(stickerSet);
        LOGGER.info("✅ Категории стикерсета {} успешно обновлены", stickerSetId);
        
        return savedStickerSet;
    }
}
