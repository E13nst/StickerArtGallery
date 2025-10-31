package com.example.sticker_art_gallery.service.category;

import com.example.sticker_art_gallery.dto.CategoryDto;
import com.example.sticker_art_gallery.dto.CreateCategoryDto;
import com.example.sticker_art_gallery.dto.CategoryWithCountDto;
import com.example.sticker_art_gallery.dto.UpdateCategoryDto;
import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.model.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для работы с категориями стикерсетов
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Получить все активные категории
     * @param language код языка для локализации
     * @return список категорий
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllActiveCategories(String language) {
        log.debug("Getting all active categories for language: {}", language);
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(category -> CategoryDto.fromEntity(category, language))
                .collect(Collectors.toList());
    }

    /**
     * Получить активные категории с количеством стикерсетов с учетом фильтров
     */
    @Transactional(readOnly = true)
    public List<CategoryWithCountDto> getActiveCategoriesWithCounts(String language, boolean officialOnly, Long authorId, boolean hasAuthorOnly) {
        log.debug("Getting active categories with counts: lang={}, officialOnly={}, authorId={}, hasAuthorOnly={}", language, officialOnly, authorId, hasAuthorOnly);

        // Берем категории по порядку отображения
        var categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        // Считаем количества
        var counts = categoryRepository.countStickerSetsByActiveCategories(officialOnly, authorId, hasAuthorOnly)
                .stream()
                .collect(java.util.stream.Collectors.toMap(CategoryRepository.CategoryCountProjection::getCategoryId, CategoryRepository.CategoryCountProjection::getCnt));

        // Мержим
        return categories.stream()
                .map(c -> new CategoryWithCountDto(
                        c.getKey(),
                        "ru".equalsIgnoreCase(language) ? c.getNameRu() : c.getNameEn(),
                        counts.getOrDefault(c.getId(), 0L)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Получить категорию по ключу
     * @param key уникальный ключ категории
     * @param language код языка для локализации
     * @return DTO категории
     * @throws IllegalArgumentException если категория не найдена
     */
    @Transactional(readOnly = true)
    public CategoryDto getCategoryByKey(String key, String language) {
        log.debug("Getting category by key: {} for language: {}", key, language);
        Category category = categoryRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Категория с ключом '" + key + "' не найдена"));
        return CategoryDto.fromEntity(category, language);
    }

    /**
     * Создать новую категорию
     * @param createDto данные для создания
     * @param language код языка для ответа
     * @return созданная категория
     * @throws IllegalArgumentException если категория с таким ключом уже существует
     */
    @Transactional
    public CategoryDto createCategory(CreateCategoryDto createDto, String language) {
        log.info("Creating new category with key: {}", createDto.getKey());
        
        // Проверка уникальности ключа
        if (categoryRepository.existsByKey(createDto.getKey())) {
            throw new IllegalArgumentException("Категория с ключом '" + createDto.getKey() + "' уже существует");
        }

        Category category = createDto.toEntity();
        Category savedCategory = categoryRepository.save(category);
        
        log.info("Category created successfully with id: {}", savedCategory.getId());
        return CategoryDto.fromEntity(savedCategory, language);
    }

    /**
     * Обновить категорию
     * @param key ключ категории
     * @param updateDto данные для обновления
     * @param language код языка для ответа
     * @return обновленная категория
     * @throws IllegalArgumentException если категория не найдена
     */
    @Transactional
    public CategoryDto updateCategory(String key, UpdateCategoryDto updateDto, String language) {
        log.info("Updating category with key: {}", key);
        
        Category category = categoryRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Категория с ключом '" + key + "' не найдена"));

        // Обновляем только переданные поля
        if (updateDto.getNameRu() != null) {
            category.setNameRu(updateDto.getNameRu());
        }
        if (updateDto.getNameEn() != null) {
            category.setNameEn(updateDto.getNameEn());
        }
        if (updateDto.getDescriptionRu() != null) {
            category.setDescriptionRu(updateDto.getDescriptionRu());
        }
        if (updateDto.getDescriptionEn() != null) {
            category.setDescriptionEn(updateDto.getDescriptionEn());
        }
        if (updateDto.getIconUrl() != null) {
            category.setIconUrl(updateDto.getIconUrl());
        }
        if (updateDto.getDisplayOrder() != null) {
            category.setDisplayOrder(updateDto.getDisplayOrder());
        }
        if (updateDto.getIsActive() != null) {
            category.setIsActive(updateDto.getIsActive());
        }

        Category updatedCategory = categoryRepository.save(category);
        
        log.info("Category updated successfully: {}", key);
        return CategoryDto.fromEntity(updatedCategory, language);
    }

    /**
     * Деактивировать категорию (мягкое удаление)
     * @param key ключ категории
     * @throws IllegalArgumentException если категория не найдена
     */
    @Transactional
    public void deactivateCategory(String key) {
        log.info("Deactivating category with key: {}", key);
        
        Category category = categoryRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Категория с ключом '" + key + "' не найдена"));

        category.setIsActive(false);
        categoryRepository.save(category);
        
        log.info("Category deactivated successfully: {}", key);
    }

    /**
     * Получить категории по списку ключей
     * @param keys список ключей
     * @return список категорий
     */
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByKeys(Set<String> keys) {
        log.debug("Getting categories by keys: {}", keys);
        List<Category> categories = categoryRepository.findByKeyInAndIsActiveTrue(keys);
        
        // Проверяем, что все запрошенные категории найдены
        Set<String> foundKeys = categories.stream()
                .map(Category::getKey)
                .collect(Collectors.toSet());
        
        Set<String> notFoundKeys = keys.stream()
                .filter(key -> !foundKeys.contains(key))
                .collect(Collectors.toSet());
        
        if (!notFoundKeys.isEmpty()) {
            throw new IllegalArgumentException("Категории не найдены: " + String.join(", ", notFoundKeys));
        }
        
        return categories;
    }

    /**
     * Получить количество стикерсетов в категории
     * @param key ключ категории
     * @return количество стикерсетов
     */
    @Transactional(readOnly = true)
    public long getStickerSetCount(String key) {
        Category category = categoryRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Категория с ключом '" + key + "' не найдена"));
        return categoryRepository.countStickerSetsByCategory(category.getId());
    }
}

