package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.service.category.CategoryService;
import com.example.sticker_art_gallery.service.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;

@Service
public class StickerSetService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetService.class);
    private final StickerSetRepository stickerSetRepository;
    private final UserService userService;
    private final TelegramBotApiService telegramBotApiService;
    private final CategoryService categoryService;
    
    @Autowired
    public StickerSetService(StickerSetRepository stickerSetRepository, UserService userService, 
                           TelegramBotApiService telegramBotApiService, CategoryService categoryService) {
        this.stickerSetRepository = stickerSetRepository;
        this.userService = userService;
        this.telegramBotApiService = telegramBotApiService;
        this.categoryService = categoryService;
    }
    
    /**
     * Создает новый стикерсет с расширенной валидацией
     * - Проверяет уникальность имени в базе данных
     * - Валидирует существование стикерсета в Telegram API
     * - Автоматически заполняет title из Telegram API если не указан
     * - Извлекает userId из initData если не указан
     */
    public StickerSet createStickerSet(CreateStickerSetDto createDto) {
        LOGGER.info("➕ Создание стикерсета с валидацией: {}", createDto.getName());
        
        // Нормализуем имя стикерсета
        createDto.normalizeName();
        String stickerSetName = createDto.getName();
        
        // 1. Проверяем, что стикерсет с таким именем еще не существует в базе
        StickerSet existingStickerSet = findByName(stickerSetName);
        if (existingStickerSet != null) {
            throw new IllegalArgumentException("Стикерсет с именем '" + stickerSetName + "' уже существует в галерее");
        }
        
        // 2. Валидируем существование стикерсета в Telegram API
        Object telegramStickerSetInfo;
        try {
            telegramStickerSetInfo = telegramBotApiService.validateStickerSetExists(stickerSetName);
            if (telegramStickerSetInfo == null) {
                throw new IllegalArgumentException("Стикерсет '" + stickerSetName + "' не найден в Telegram");
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при валидации стикерсета в Telegram API: {}", e.getMessage());
            throw new IllegalArgumentException("Не удалось проверить существование стикерсета в Telegram: " + e.getMessage());
        }
        
        // 3. Определяем userId
        Long userId = createDto.getUserId();
        if (userId == null) {
            userId = extractUserIdFromAuthentication();
            if (userId == null) {
                throw new IllegalArgumentException("Не удалось определить ID пользователя. Укажите userId или убедитесь, что вы авторизованы через Telegram Web App");
            }
            LOGGER.debug("📱 Извлечен userId из аутентификации: {}", userId);
        }
        
        // 4. Определяем title
        String title = createDto.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = telegramBotApiService.extractTitleFromStickerSetInfo(telegramStickerSetInfo);
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Не удалось получить название стикерсета из Telegram API");
            }
            LOGGER.debug("📝 Получен title из Telegram API: '{}'", title);
        }
        
        // 5. Обрабатываем категории
        List<Category> categories = null;
        if (createDto.getCategoryKeys() != null && !createDto.getCategoryKeys().isEmpty()) {
            try {
                categories = categoryService.getCategoriesByKeys(createDto.getCategoryKeys());
                LOGGER.debug("📁 Найдено категорий: {}", categories.size());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("⚠️ Ошибка при получении категорий: {}", e.getMessage());
                throw e;
            }
        }
        
        // 6. Создаем стикерсет
        return createStickerSetInternal(userId, title, stickerSetName, categories);
    }
    
    /**
     * Внутренний метод для создания стикерсета без валидации
     */
    private StickerSet createStickerSetInternal(Long userId, String title, String name, List<Category> categories) {
        // Автоматически создаем пользователя, если его нет
        try {
            userService.findOrCreateByTelegramId(userId, null, null, null, null);
            LOGGER.info("✅ Пользователь {} автоматически создан/найден при создании стикерпака", userId);
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось создать/найти пользователя {}: {}", userId, e.getMessage());
        }
        
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle(title);
        stickerSet.setName(name);
        
        // Добавляем категории, если они указаны
        if (categories != null && !categories.isEmpty()) {
            for (Category category : categories) {
                stickerSet.addCategory(category);
            }
            LOGGER.debug("📁 Добавлено категорий к стикерсету: {}", categories.size());
        }

        StickerSet savedSet = stickerSetRepository.save(stickerSet);
        LOGGER.info("📦 Создан стикерпак: ID={}, Title='{}', Name='{}', UserId={}, Categories={}", 
                savedSet.getId(), title, name, userId, 
                savedSet.getCategories() != null ? savedSet.getCategories().size() : 0);

        return savedSet;
    }
    
    /**
     * Извлекает userId из текущей аутентификации
     */
    private Long extractUserIdFromAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.example.sticker_art_gallery.model.user.UserEntity) {
                com.example.sticker_art_gallery.model.user.UserEntity user = 
                    (com.example.sticker_art_gallery.model.user.UserEntity) authentication.getPrincipal();
                return user.getId();
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при извлечении userId из аутентификации: {}", e.getMessage());
            return null;
        }
    }

    public StickerSet findByName(String name) {
        return stickerSetRepository.findByName(name).orElse(null);
    }

    public StickerSet findByTitle(String title) {
        return stickerSetRepository.findByTitle(title);
    }

    public List<StickerSet> findByUserId(Long userId) {
        return stickerSetRepository.findByUserId(userId);
    }

    public StickerSet findById(Long id) {
        return stickerSetRepository.findById(id).orElse(null);
    }
    
    public List<StickerSet> findAll() {
        return stickerSetRepository.findAll();
    }
    
    public StickerSet save(StickerSet stickerSet) {
        // Автоматически создаем пользователя, если его нет
        try {
            userService.findOrCreateByTelegramId(stickerSet.getUserId(), null, null, null, null);
            LOGGER.info("✅ Пользователь {} автоматически создан/найден при сохранении стикерпака", stickerSet.getUserId());
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось создать/найти пользователя {}: {}", stickerSet.getUserId(), e.getMessage());
        }
        
        return stickerSetRepository.save(stickerSet);
    }
    
    public void deleteById(Long id) {
        stickerSetRepository.deleteById(id);
    }
    
    /**
     * Получить все стикерсеты с пагинацией и обогащением данных Bot API
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language) {
        LOGGER.debug("📋 Получение всех стикерсетов с пагинацией: page={}, size={}, language={}", 
                pageRequest.getPage(), pageRequest.getSize(), language);
        
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findAll(pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language);
        
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсеты пользователя с пагинацией и обогащением данных Bot API
     */
    public PageResponse<StickerSetDto> findByUserIdWithPagination(Long userId, PageRequest pageRequest) {
        LOGGER.debug("👤 Получение стикерсетов пользователя {} с пагинацией: page={}, size={}", 
                userId, pageRequest.getPage(), pageRequest.getSize());
        
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findByUserId(userId, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiData(stickerSetsPage.getContent());
        
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсеты по ключам категорий с пагинацией и обогащением данных Bot API
     */
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language) {
        LOGGER.debug("🏷️ Получение стикерсетов по категориям {} с пагинацией: page={}, size={}", 
                String.join(",", categoryKeys), pageRequest.getPage(), pageRequest.getSize());
        
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findByCategoryKeys(categoryKeys, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language);
        
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсет по ID с обогащением данных Bot API
     * Если Bot API недоступен, возвращает стикерсет без обогащения
     */
    public StickerSetDto findByIdWithBotApiData(Long id) {
        LOGGER.debug("🔍 Получение стикерсета по ID {} с данными Bot API", id);
        
        StickerSet stickerSet = stickerSetRepository.findById(id).orElse(null);
        if (stickerSet == null) {
            return null;
        }
        
        return enrichSingleStickerSetSafely(stickerSet);
    }
    
    /**
     * Получить стикерсет по имени с обогащением данных Bot API
     * Если Bot API недоступен, возвращает стикерсет без обогащения
     */
    public StickerSetDto findByNameWithBotApiData(String name) {
        LOGGER.debug("🔍 Получение стикерсета по имени '{}' с данными Bot API", name);
        
        StickerSet stickerSet = stickerSetRepository.findByName(name).orElse(null);
        if (stickerSet == null) {
            return null;
        }
        
        return enrichSingleStickerSetSafely(stickerSet);
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
    
    /**
     * Обогащает список стикерсетов данными из Bot API и категориями (последовательно для Hibernate)
     */
    private List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language) {
        if (stickerSets.isEmpty()) {
            return List.of();
        }
        
        LOGGER.debug("🚀 Обогащение {} стикерсетов данными Bot API и категориями (последовательно)", stickerSets.size());
        
        // Обрабатываем последовательно, чтобы избежать проблем с Hibernate Session
        List<StickerSetDto> result = stickerSets.stream()
                .map(stickerSet -> enrichSingleStickerSetSafelyWithCategories(stickerSet, language))
                .collect(Collectors.toList());
        
        LOGGER.debug("✅ Обогащение завершено для {} стикерсетов", result.size());
        return result;
    }
    
    /**
     * Обогащает список стикерсетов данными из Bot API (параллельно)
     */
    private List<StickerSetDto> enrichWithBotApiData(List<StickerSet> stickerSets) {
        return enrichWithBotApiDataAndCategories(stickerSets, "en");
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API и категориями (безопасно)
     */
    private StickerSetDto enrichSingleStickerSetSafelyWithCategories(StickerSet stickerSet, String language) {
        StickerSetDto dto = StickerSetDto.fromEntity(stickerSet, language);
        
        try {
            Object botApiData = telegramBotApiService.getStickerSetInfo(stickerSet.getName());
            dto.setTelegramStickerSetInfo(botApiData);
            LOGGER.debug("✅ Стикерсет '{}' обогащен данными Bot API", stickerSet.getName());
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось получить данные Bot API для стикерсета '{}': {} - пропускаем обогащение", 
                    stickerSet.getName(), e.getMessage());
            // Оставляем telegramStickerSetInfo = null, продолжаем обработку
            dto.setTelegramStickerSetInfo(null);
        }
        
        return dto;
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API (безопасно)
     * Если данные Bot API недоступны, возвращает DTO без обогащения, но не выбрасывает исключение
     */
    private StickerSetDto enrichSingleStickerSetSafely(StickerSet stickerSet) {
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, "en");
    }
} 