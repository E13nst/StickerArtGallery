package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.service.category.CategoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;

@Service
public class StickerSetService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetService.class);
    private final StickerSetRepository stickerSetRepository;
    private final TelegramBotApiService telegramBotApiService;
    private final CategoryService categoryService;
    
    @Autowired
    public StickerSetService(StickerSetRepository stickerSetRepository, 
                           TelegramBotApiService telegramBotApiService, CategoryService categoryService) {
        this.stickerSetRepository = stickerSetRepository;
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
        
        // 1. Проверяем, что стикерсет с таким именем или URL уже не существует в базе (игнорируя регистр)
        Optional<StickerSet> existingByName = stickerSetRepository.findByNameIgnoreCase(stickerSetName);
        if (existingByName.isPresent()) {
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
        
        // 4.1 Проверяем, что стикерсет с таким title не существует (игнорируя регистр)
        Optional<StickerSet> existingByTitle = stickerSetRepository.findByTitleIgnoreCase(title);
        if (existingByTitle.isPresent()) {
            throw new IllegalArgumentException("Стикерсет с названием '" + title + "' уже существует в галерее");
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
        // Профиль пользователя создается автоматически при аутентификации
        LOGGER.debug("Создание стикерсета для пользователя {}", userId);
        
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
            if (authentication != null && authentication.isAuthenticated()) {
                // Principal теперь содержит telegramId в getName()
                return Long.valueOf(authentication.getName());
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
        // Профиль пользователя создается автоматически при аутентификации
        LOGGER.debug("Сохранение стикерсета для пользователя {}", stickerSet.getUserId());
        
        return stickerSetRepository.save(stickerSet);
    }
    
    public void deleteById(Long id) {
        stickerSetRepository.deleteById(id);
    }
    
    /**
     * Получить все стикерсеты с пагинацией и обогащением данных Bot API
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language) {
        return findAllWithPagination(pageRequest, language, null);
    }
    
    /**
     * Получить все стикерсеты с пагинацией и обогащением данных Bot API
     * Возвращает только публичные и не заблокированные стикерсеты для галереи
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId) {
        LOGGER.debug("📋 Получение публичных и не заблокированных стикерсетов с пагинацией: page={}, size={}, language={}", 
                pageRequest.getPage(), pageRequest.getSize(), language);
        
        // Получаем только публичные и не заблокированные стикерсеты для галереи
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findPublicAndNotBlocked(pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId);
        
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить все стикерсеты с опциональной фильтрацией по официальным
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId, boolean officialOnly) {
        LOGGER.debug("📋 Получение {} стикерсетов с пагинацией: page={}, size={}, language={}", 
                officialOnly ? "официальных" : "публичных", pageRequest.getPage(), pageRequest.getSize(), language);
        Page<StickerSet> stickerSetsPage = officialOnly
                ? stickerSetRepository.findPublicNotBlockedAndOfficial(pageRequest.toPageable())
                : stickerSetRepository.findPublicAndNotBlocked(pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId);
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить все стикерсеты с дополнительной фильтрацией по автору
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             boolean officialOnly, Long authorId, boolean hasAuthorOnly) {
        LOGGER.debug("📋 Получение {} стикерсетов{} с пагинацией: page={}, size={}, language={}",
                officialOnly ? "официальных" : "публичных",
                authorId != null ? (" автора=" + authorId) : (hasAuthorOnly ? " (только с автором)" : ""),
                pageRequest.getPage(), pageRequest.getSize(), language);
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findPublicNotBlockedFiltered(officialOnly, authorId, hasAuthorOnly, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId);
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
        return findByCategoryKeys(categoryKeys, pageRequest, language, null);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId) {
        LOGGER.debug("🏷️ Получение публичных и не заблокированных стикерсетов по категориям {} с пагинацией: page={}, size={}", 
                String.join(",", categoryKeys), pageRequest.getPage(), pageRequest.getSize());
        
        // Получаем только публичные и не заблокированные стикерсеты для галереи
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findByCategoryKeysPublicAndNotBlocked(categoryKeys, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId);
        
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсеты по ключам категорий с опциональной фильтрацией по официальным
     */
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId, boolean officialOnly) {
        LOGGER.debug("🏷️ Получение {} стикерсетов по категориям {} с пагинацией: page={}, size={}", 
                officialOnly ? "официальных" : "публичных", String.join(",", categoryKeys), pageRequest.getPage(), pageRequest.getSize());
        Page<StickerSet> stickerSetsPage = officialOnly
                ? stickerSetRepository.findByCategoryKeysPublicNotBlockedAndOfficial(categoryKeys, pageRequest.toPageable())
                : stickerSetRepository.findByCategoryKeysPublicAndNotBlocked(categoryKeys, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId);
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсеты по категориям с дополнительной фильтрацией по автору
     */
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          boolean officialOnly, Long authorId, boolean hasAuthorOnly) {
        LOGGER.debug("🏷️ Получение {} стикерсетов по категориям {}{} с пагинацией: page={}, size={}",
                officialOnly ? "официальных" : "публичных",
                String.join(",", categoryKeys),
                authorId != null ? (" автора=" + authorId) : (hasAuthorOnly ? " (только с автором)" : ""),
                pageRequest.getPage(), pageRequest.getSize());
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findByCategoryKeysPublicNotBlockedFiltered(categoryKeys, officialOnly, authorId, hasAuthorOnly, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId);
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
     * Получить стикерсет по ID с обогащением данных Bot API и информацией о лайке текущего пользователя
     * Если Bot API недоступен, возвращает стикерсет без обогащения
     * @param id ID стикерсета
     * @param currentUserId ID текущего пользователя (может быть null для неавторизованных пользователей)
     * @return StickerSetDto с полем isLikedByCurrentUser
     */
    public StickerSetDto findByIdWithBotApiData(Long id, Long currentUserId) {
        LOGGER.debug("🔍 Получение стикерсета по ID {} с данными Bot API и информацией о лайке пользователя {}", id, currentUserId);
        
        StickerSet stickerSet = stickerSetRepository.findById(id).orElse(null);
        if (stickerSet == null) {
            return null;
        }
        
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, "en", currentUserId);
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
     * Изменить видимость стикерсета (публичный/приватный)
     */
    @Transactional
    public StickerSet updateVisibility(Long stickerSetId, Boolean isPublic) {
        LOGGER.info("👁️ Изменение видимости стикерсета ID: {} на {}", stickerSetId, isPublic ? "публичный" : "приватный");
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        stickerSet.setIsPublic(isPublic);
        
        StickerSet savedStickerSet = stickerSetRepository.save(stickerSet);
        LOGGER.info("✅ Видимость стикерсета {} успешно изменена на {}", stickerSetId, isPublic ? "публичный" : "приватный");
        
        return savedStickerSet;
    }
    
    /**
     * Заблокировать стикерсет (только для админа)
     */
    @Transactional
    public StickerSet blockStickerSet(Long stickerSetId, String reason) {
        LOGGER.info("🚫 Блокировка стикерсета ID: {}, причина: {}", stickerSetId, reason);
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        stickerSet.setIsBlocked(true);
        stickerSet.setBlockReason(reason);
        
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
        
        stickerSet.setIsBlocked(false);
        stickerSet.setBlockReason(null);
        
        StickerSet savedStickerSet = stickerSetRepository.save(stickerSet);
        LOGGER.info("✅ Стикерсет {} успешно разблокирован", stickerSetId);
        
        return savedStickerSet;
    }
    
    /**
     * Установить официальный статус стикерсета (только для админа)
     */
    @Transactional
    public StickerSet setOfficial(Long stickerSetId) {
        LOGGER.info("🏅 Установка статуса ОФИЦИАЛЬНЫЙ для стикерсета ID: {}", stickerSetId);
        
        StickerSet stickerSet = stickerSetRepository.findById(stickerSetId)
            .orElseThrow(() -> new IllegalArgumentException("Стикерсет с ID " + stickerSetId + " не найден"));
        
        stickerSet.setIsOfficial(true);
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
        
        stickerSet.setIsOfficial(false);
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
        return enrichWithBotApiDataAndCategories(stickerSets, language, null);
    }
    
    /**
     * Обогащает список стикерсетов данными из Bot API и категориями (последовательно для Hibernate)
     */
    private List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId) {
        if (stickerSets.isEmpty()) {
            return List.of();
        }
        
        LOGGER.debug("🚀 Обогащение {} стикерсетов данными Bot API и категориями (последовательно)", stickerSets.size());
        
        // Обрабатываем последовательно, чтобы избежать проблем с Hibernate Session
        List<StickerSetDto> result = stickerSets.stream()
                .map(stickerSet -> enrichSingleStickerSetSafelyWithCategories(stickerSet, language, currentUserId))
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
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, language, null);
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API и категориями (безопасно)
     */
    private StickerSetDto enrichSingleStickerSetSafelyWithCategories(StickerSet stickerSet, String language, Long currentUserId) {
        StickerSetDto dto = StickerSetDto.fromEntity(stickerSet, language, currentUserId);
        
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