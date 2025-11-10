package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.service.category.CategoryService;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
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
    private final ArtRewardService artRewardService;
    
    @Autowired
    public StickerSetService(StickerSetRepository stickerSetRepository,
                             TelegramBotApiService telegramBotApiService,
                             CategoryService categoryService,
                             ArtRewardService artRewardService) {
        this.stickerSetRepository = stickerSetRepository;
        this.telegramBotApiService = telegramBotApiService;
        this.categoryService = categoryService;
        this.artRewardService = artRewardService;
    }
    
    /**
     * Создает новый стикерсет с расширенной валидацией
     * - Проверяет уникальность имени в базе данных
     * - Валидирует существование стикерсета в Telegram API
     * - Автоматически заполняет title из Telegram API если не указан
     * - Извлекает userId из initData если не указан
     */
    public StickerSet createStickerSet(CreateStickerSetDto createDto, String language) {
        String lang = normalizeLanguage(language);
        Long userId = extractUserIdFromAuthentication();
        if (userId == null) {
            throw new IllegalArgumentException(localize(
                    lang,
                    "Не удалось определить ID пользователя. Убедитесь, что вы авторизованы через Telegram Web App",
                    "Failed to determine user ID. Make sure you are authorized via Telegram Web App"
            ));
        }
        LOGGER.debug("📱 Извлечен userId из аутентификации: {}", userId);
        return createStickerSetForUser(createDto, userId, lang);
    }

    /**
     * Создает стикерсет от имени конкретного пользователя (используется межсервисным API).
     */
    public StickerSet createStickerSetForUser(CreateStickerSetDto createDto, Long userId, String language) {
        String lang = normalizeLanguage(language);
        if (userId == null) {
            throw new IllegalArgumentException(localize(
                    lang,
                    "Требуется ID пользователя для создания стикерсета",
                    "User ID is required to create a stickerset"
            ));
        }
        LOGGER.info("➕ Создание стикерсета для пользователя {}: {}", userId, createDto.getName());
        return createStickerSetValidated(createDto, userId, lang);
    }

    private StickerSet createStickerSetValidated(CreateStickerSetDto createDto, Long userId, String lang) {
        // Нормализуем имя стикерсета
        createDto.normalizeName();
        String stickerSetName = createDto.getName();

        if (createDto.getIsPublic() == null) {
            createDto.setIsPublic(true);
        }

        // 1. Проверяем, что стикерсет с таким именем или URL уже не существует в базе (игнорируя регистр)
        Optional<StickerSet> existingByName = Optional.ofNullable(
                stickerSetRepository.findByNameIgnoreCase(stickerSetName)
        ).orElse(Optional.empty());
        if (existingByName.isPresent()) {
            throw new IllegalArgumentException(localize(
                    lang,
                    "Стикерсет с именем '" + stickerSetName + "' уже существует в галерее",
                    "A stickerset with the name '" + stickerSetName + "' already exists in the gallery"
            ));
        }

        // 2. Валидируем существование стикерсета в Telegram API
        Object telegramStickerSetInfo;
        try {
            telegramStickerSetInfo = telegramBotApiService.validateStickerSetExists(stickerSetName);
            if (telegramStickerSetInfo == null) {
                throw new IllegalArgumentException(localize(
                        lang,
                        "Стикерсет '" + stickerSetName + "' не найден в Telegram",
                        "Stickerset '" + stickerSetName + "' was not found in Telegram"
                ));
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при валидации стикерсета в Telegram API: {}", e.getMessage());
            throw new IllegalArgumentException(localize(
                    lang,
                    "Не удалось проверить существование стикерсета в Telegram: " + e.getMessage(),
                    "Failed to verify stickerset existence in Telegram: " + e.getMessage()
            ));
        }

        // 3. Определяем title
        String title = createDto.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = telegramBotApiService.extractTitleFromStickerSetInfo(telegramStickerSetInfo);
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException(localize(
                        lang,
                        "Не удалось получить название стикерсета из Telegram API",
                        "Failed to retrieve stickerset title from Telegram API"
                ));
            }
            LOGGER.debug("📝 Получен title из Telegram API: '{}'", title);
        }

        // 4. Обрабатываем категории
        List<Category> categories = null;
        if (createDto.getCategoryKeys() != null && !createDto.getCategoryKeys().isEmpty()) {
            try {
                categories = categoryService.getCategoriesByKeys(createDto.getCategoryKeys());
                LOGGER.debug("📁 Найдено категорий: {}", categories.size());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("⚠️ Ошибка при получении категорий: {}", e.getMessage());
                throw new IllegalArgumentException(localize(
                        lang,
                        e.getMessage(),
                        e.getMessage()
                ));
            }
        }

        // 5. Создаем стикерсет
        return createStickerSetInternal(userId, title, stickerSetName, createDto.getIsPublic(), categories);
    }
    
    /**
     * Внутренний метод для создания стикерсета без валидации
     */
    private StickerSet createStickerSetInternal(Long userId, String title, String name, Boolean isPublic, List<Category> categories) {
        // Профиль пользователя создается автоматически при аутентификации
        LOGGER.debug("Создание стикерсета для пользователя {}", userId);
        
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle(title);
        stickerSet.setName(name);
        stickerSet.setIsPublic(Boolean.TRUE.equals(isPublic));
        
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

        try {
            String metadata = String.format("{\"stickerSetId\":%d}", savedSet.getId());
            String externalId = String.format("sticker-upload:%d:%d", userId, savedSet.getId());
            artRewardService.award(
                    userId,
                    ArtRewardService.RULE_UPLOAD_STICKERSET,
                    null,
                    metadata,
                    externalId,
                    userId
            );
            LOGGER.info("💎 Начислены ART за создание стикерсета: userId={}, stickerSetId={}", userId, savedSet.getId());
        } catch (Exception e) {
            LOGGER.error("❌ Не удалось начислить ART пользователю {} за стикерсет {}: {}",
                    userId, savedSet.getId(), e.getMessage(), e);
        }

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

    private String normalizeLanguage(String language) {
        if (language == null) {
            return "en";
        }
        String normalized = language.trim().toLowerCase();
        return ("ru".equals(normalized)) ? "ru" : "en";
    }

    private String localize(String language, String ruMessage, String enMessage) {
        return "ru".equals(language) ? ruMessage : enMessage;
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
     * @param userId ID пользователя, чьи стикерсеты запрашиваются
     * @param pageRequest параметры пагинации/сортировки
     * @param categoryKeys фильтр по категориям (может быть null или пустым)
     * @param hasAuthorOnly показать только стикерсеты с указанным автором
     * @param likedOnly показать только стикерсеты, лайкнутые текущим пользователем
     * @param currentUserId ID текущего авторизованного пользователя (может быть null)
     * @param includePrivate показывать ли приватные стикерсеты (true для владельца или администратора)
     */
    public PageResponse<StickerSetDto> findByUserIdWithPagination(Long userId,
                                                                  PageRequest pageRequest,
                                                                  Set<String> categoryKeys,
                                                                  boolean hasAuthorOnly,
                                                                  boolean likedOnly,
                                                                  Long currentUserId,
                                                                  boolean includePrivate) {
        LOGGER.debug("👤 Получение стикерсетов пользователя {} с пагинацией: page={}, size={}, hasAuthorOnly={}, likedOnly={}, includePrivate={}, categoryKeys={}",
                userId, pageRequest.getPage(), pageRequest.getSize(), hasAuthorOnly, likedOnly, includePrivate,
                categoryKeys == null ? "null" : String.join(",", categoryKeys));

        Set<String> normalizedCategoryKeys = (categoryKeys == null || categoryKeys.isEmpty()) ? null : categoryKeys;

        Page<StickerSet> stickerSetsPage = stickerSetRepository.findUserStickerSetsFiltered(
                userId,
                includePrivate,
                hasAuthorOnly,
                normalizedCategoryKeys,
                likedOnly,
                currentUserId,
                pageRequest.toPageable()
        );

        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), "en", currentUserId);

        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить авторские стикерсеты с пагинацией и обогащением данных
     */
    public PageResponse<StickerSetDto> findByAuthorIdWithPagination(Long authorId,
                                                                    PageRequest pageRequest,
                                                                    Set<String> categoryKeys,
                                                                    Long currentUserId,
                                                                    boolean includePrivate) {
        LOGGER.debug("✍️ Получение авторских стикерсетов {} с пагинацией: page={}, size={}, includePrivate={}, categoryKeys={}",
                authorId, pageRequest.getPage(), pageRequest.getSize(), includePrivate,
                categoryKeys == null ? "null" : String.join(",", categoryKeys));

        Set<String> normalizedCategoryKeys = (categoryKeys == null || categoryKeys.isEmpty()) ? null : categoryKeys;

        Page<StickerSet> stickerSetsPage = stickerSetRepository.findAuthorStickerSetsFiltered(
                authorId,
                includePrivate,
                normalizedCategoryKeys,
                pageRequest.toPageable()
        );

        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), "en", currentUserId);

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
        return findByIdWithBotApiData(id, null, null);
    }
    
    /**
     * Получить стикерсет по ID с обогащением данных Bot API и информацией о лайке текущего пользователя
     * Если Bot API недоступен, возвращает стикерсет без обогащения
     * @param id ID стикерсета
     * @param currentUserId ID текущего пользователя (может быть null для неавторизованных пользователей)
     * @return StickerSetDto с полем isLikedByCurrentUser
     */
    public StickerSetDto findByIdWithBotApiData(Long id, Long currentUserId) {
        return findByIdWithBotApiData(id, null, currentUserId);
    }
    
    /**
     * Получить стикерсет по ID с учётом языка и лайков пользователя
     */
    public StickerSetDto findByIdWithBotApiData(Long id, String language, Long currentUserId) {
        LOGGER.debug("🔍 Получение стикерсета по ID {} с данными Bot API (language={}, currentUserId={})", id, language, currentUserId);
        
        StickerSet stickerSet = stickerSetRepository.findById(id).orElse(null);
        if (stickerSet == null) {
            return null;
        }
        
        String lang = normalizeLanguage(language);
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, lang, currentUserId);
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