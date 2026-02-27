package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.service.category.CategoryService;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.LikeService;
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
import jakarta.transaction.Transactional;

@Service
public class StickerSetService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetService.class);
    private final StickerSetRepository stickerSetRepository;
    private final TelegramBotApiService telegramBotApiService;
    private final CategoryService categoryService;
    private final ArtRewardService artRewardService;
    private final StickerSetCrudService crudService;
    private final StickerSetVisibilityService visibilityService;
    private final StickerSetEnrichmentService enrichmentService;
    private final StickerSetNamingService namingService;
    private LikeService likeService; // Lazy injection to avoid circular dependency

    @Autowired
    public StickerSetService(StickerSetRepository stickerSetRepository,
                             TelegramBotApiService telegramBotApiService,
                             CategoryService categoryService,
                             ArtRewardService artRewardService,
                             StickerSetCrudService crudService,
                             StickerSetVisibilityService visibilityService,
                             StickerSetEnrichmentService enrichmentService,
                             StickerSetNamingService namingService) {
        this.stickerSetRepository = stickerSetRepository;
        this.telegramBotApiService = telegramBotApiService;
        this.categoryService = categoryService;
        this.artRewardService = artRewardService;
        this.crudService = crudService;
        this.visibilityService = visibilityService;
        this.enrichmentService = enrichmentService;
        this.namingService = namingService;
    }
    
    @Autowired(required = false)
    public void setLikeService(LikeService likeService) {
        this.likeService = likeService;
    }
    
    /**
     * Создает новый стикерсет с расширенной валидацией
     * - Проверяет уникальность имени в базе данных
     * - Валидирует существование стикерсета в Telegram API
     * - Автоматически заполняет title из Telegram API если не указан
     * - Извлекает userId из initData если не указан
     * - Устанавливает visibility = PUBLIC по умолчанию для публичного API
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
        
        // Устанавливаем visibility = PUBLIC по умолчанию для публичного API
        if (createDto.getVisibility() == null) {
            // Проверяем обратную совместимость через isPublic
            if (createDto.getIsPublic() != null) {
                createDto.setVisibility(createDto.getIsPublic() ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE);
            } else {
                createDto.setVisibility(StickerSetVisibility.PUBLIC);
            }
        }
        
        return createStickerSetForUser(createDto, userId, lang, true);
    }

    /**
     * Создает стикерсет от имени конкретного пользователя (используется межсервисным API).
     * Устанавливает visibility = PRIVATE по умолчанию для internal API.
     */
    public StickerSet createStickerSetForUser(CreateStickerSetDto createDto, Long userId, String language, Long authorId) {
        // Deprecated authorId: при authorId != null интерпретируется как isVerified=true
        return createStickerSetForUser(createDto, userId, language, authorId != null);
    }

    public StickerSet createStickerSetForUser(CreateStickerSetDto createDto, Long userId, String language, boolean isVerified) {
        String lang = normalizeLanguage(language);
        if (userId == null) {
            throw new IllegalArgumentException(localize(
                    lang,
                    "Требуется ID пользователя для создания стикерсета",
                    "User ID is required to create a stickerset"
            ));
        }
        LOGGER.info("➕ Создание стикерсета для пользователя {} (isVerified={}): {}", userId, isVerified, createDto.getName());
        
        // Устанавливаем visibility = PRIVATE по умолчанию для internal API
        if (createDto.getVisibility() == null) {
            // Проверяем обратную совместимость через isPublic
            if (createDto.getIsPublic() != null) {
                createDto.setVisibility(createDto.getIsPublic() ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE);
            } else {
                createDto.setVisibility(StickerSetVisibility.PRIVATE);
            }
        }
        
        return createStickerSetValidated(createDto, userId, lang, isVerified);
    }

    private StickerSet createStickerSetValidated(CreateStickerSetDto createDto, Long userId, String lang, boolean isVerified) {
        // Нормализуем имя стикерсета и гарантируем суффикс _by_<bot>
        createDto.normalizeName();
        String stickerSetName = createDto.getName();
        if (stickerSetName != null && !stickerSetName.isBlank()) {
            stickerSetName = namingService.ensureBotSuffix(stickerSetName);
            createDto.setName(stickerSetName);
        }

        // 1. Проверяем существующий стикерсет с таким именем (игнорируя регистр)
        Optional<StickerSet> existingByName = Optional.ofNullable(
                stickerSetRepository.findByNameIgnoreCase(stickerSetName)
        ).orElse(Optional.empty());
        
        if (existingByName.isPresent()) {
            StickerSet existing = existingByName.get();
            
            // Если BLOCKED - запрещаем повторную загрузку
            if (existing.isBlocked()) {
                String reason = existing.getBlockReason() != null 
                    ? existing.getBlockReason() 
                    : localize(lang, "Причина не указана", "Reason not specified");
                throw new IllegalArgumentException(localize(
                        lang,
                        "Стикерсет '" + stickerSetName + "' был заблокирован. Причина: " + reason,
                        "Stickerset '" + stickerSetName + "' was blocked. Reason: " + reason
                ));
            }
            
            // Если ACTIVE - уже существует
            if (existing.isActive()) {
                throw new IllegalArgumentException(localize(
                        lang,
                        "Стикерсет с именем '" + stickerSetName + "' уже существует в галерее",
                        "A stickerset with the name '" + stickerSetName + "' already exists in the gallery"
                ));
            }
            
            // Если DELETED - восстанавливаем запись (обновляем старую запись)
            if (existing.isDeleted()) {
                LOGGER.info("🔄 Восстановление удаленного стикерсета: ID={}, Name={}", existing.getId(), stickerSetName);
                return restoreAndUpdateStickerSet(existing, createDto, userId, lang, isVerified);
            }
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

        // 4. Извлекаем количество стикеров
        Integer stickersCount = telegramBotApiService.extractStickersCountFromStickerSetInfo(telegramStickerSetInfo);
        LOGGER.debug("📊 Получено количество стикеров из Telegram API: {}", stickersCount);

        // 5. Обрабатываем категории
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

        // 6. Создаем стикерсет
        return createStickerSetInternal(userId, title, stickerSetName, createDto.getDescription(), createDto.getVisibility(), categories, isVerified, false, stickersCount);
    }
    
    /**
     * Восстановление и обновление удаленного стикерсета
     */
    private StickerSet restoreAndUpdateStickerSet(StickerSet existing, CreateStickerSetDto createDto, 
                                                   Long userId, String lang, boolean isVerified) {
        String stickerSetName = createDto.getName();
        
        // 1. Валидируем в Telegram API (может быть удален там)
        Object telegramStickerSetInfo;
        try {
            telegramStickerSetInfo = telegramBotApiService.validateStickerSetExists(stickerSetName);
            if (telegramStickerSetInfo == null) {
                throw new IllegalArgumentException(localize(
                        lang,
                        "Стикерсет не найден в Telegram",
                        "Stickerset was not found in Telegram"
                ));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(localize(
                    lang,
                    "Не удалось проверить существование стикерсета в Telegram: " + e.getMessage(),
                    "Failed to verify stickerset existence in Telegram: " + e.getMessage()
            ));
        }
        
        // 2. Восстанавливаем статус
        existing.restore();
        
        // 3. Обновляем данные
        existing.setUserId(userId);
        existing.setVisibility(createDto.getVisibility() != null ? createDto.getVisibility() : StickerSetVisibility.PRIVATE);
        existing.setIsVerified(isVerified);
        
        // 4. Обновляем title если указан
        if (createDto.getTitle() != null && !createDto.getTitle().trim().isEmpty()) {
            existing.setTitle(createDto.getTitle());
        } else {
            String title = telegramBotApiService.extractTitleFromStickerSetInfo(telegramStickerSetInfo);
            if (title != null && !title.trim().isEmpty()) {
                existing.setTitle(title);
            }
        }
        
        // Обновляем количество стикеров
        Integer stickersCount = telegramBotApiService.extractStickersCountFromStickerSetInfo(telegramStickerSetInfo);
        existing.setStickersCount(stickersCount);
        LOGGER.debug("📊 Обновлено количество стикеров при восстановлении: {}", stickersCount);
        
        // Обновляем description если указан
        if (createDto.getDescription() != null) {
            existing.setDescription(createDto.getDescription().trim().isEmpty() ? null : createDto.getDescription());
        }
        
        // 5. Обновляем категории
        if (createDto.getCategoryKeys() != null && !createDto.getCategoryKeys().isEmpty()) {
            List<Category> categories = categoryService.getCategoriesByKeys(createDto.getCategoryKeys());
            existing.clearCategories();
            for (Category category : categories) {
                existing.addCategory(category);
            }
        }
        
        StickerSet savedSet = stickerSetRepository.save(existing);
        LOGGER.info("✅ Восстановлен стикерсет: ID={}, Name={}, UserId={}, Visibility={}", 
                savedSet.getId(), savedSet.getName(), userId, savedSet.getVisibility());
        
        // НЕ начисляем ART - это восстановление, не новый стикерсет
        
        return savedSet;
    }
    
    /**
     * Внутренний метод для создания стикерсета без валидации
     * @param isRestored флаг, указывающий что это восстановление (не начислять ART)
     * @param stickersCount количество стикеров в стикерсете
     */
    private StickerSet createStickerSetInternal(Long userId,
                                               String title,
                                               String name,
                                               String description,
                                               StickerSetVisibility visibility,
                                               List<Category> categories,
                                               boolean isVerified,
                                               boolean isRestored,
                                               Integer stickersCount) {
        // Профиль пользователя создается автоматически при аутентификации
        LOGGER.debug("Создание стикерсета для пользователя {}", userId);
        
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle(title);
        stickerSet.setName(name);
        stickerSet.setDescription(description);
        stickerSet.setState(StickerSetState.ACTIVE);
        stickerSet.setVisibility(visibility != null ? visibility : StickerSetVisibility.PRIVATE);
        stickerSet.setType(StickerSetType.USER);
        stickerSet.setStickersCount(stickersCount);
        stickerSet.setIsVerified(isVerified);
        
        // Добавляем категории, если они указаны
        if (categories != null && !categories.isEmpty()) {
            for (Category category : categories) {
                stickerSet.addCategory(category);
            }
            LOGGER.debug("📁 Добавлено категорий к стикерсету: {}", categories.size());
        }

        StickerSet savedSet = stickerSetRepository.save(stickerSet);
        LOGGER.info("📦 Создан стикерсет: ID={}, Title='{}', Name='{}', UserId={}, Visibility={}, Categories={}", 
                savedSet.getId(), title, name, userId, savedSet.getVisibility(),
                savedSet.getCategories() != null ? savedSet.getCategories().size() : 0);

        // Начисляем ART только для НОВЫХ стикерсетов (не восстановленных) И только если PUBLIC
        if (!isRestored && savedSet.getVisibility() == StickerSetVisibility.PUBLIC) {
            try {
                String metadata = String.format("{\"stickerSetId\":%d,\"name\":\"%s\"}", savedSet.getId(), name);
                String externalId = String.format("sticker-upload:%d:%d", userId, savedSet.getId());
                artRewardService.award(
                        userId,
                        ArtRewardService.RULE_UPLOAD_STICKERSET,
                        null,
                        metadata,
                        externalId,
                        userId
                );
                LOGGER.info("💎 Начислены ART за создание публичного стикерсета: userId={}, stickerSetId={}", userId, savedSet.getId());
            } catch (Exception e) {
                LOGGER.error("❌ Не удалось начислить ART пользователю {} за стикерсет {}: {}",
                        userId, savedSet.getId(), e.getMessage(), e);
            }
        } else {
            LOGGER.debug("♻️ ART не начисляются: isRestored={}, visibility={}", isRestored, savedSet.getVisibility());
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
        return crudService.findByName(name);
    }

    public StickerSet findByTitle(String title) {
        return crudService.findByTitle(title);
    }

    public List<StickerSet> findByUserId(Long userId) {
        return crudService.findByUserId(userId);
    }

    public StickerSet findById(Long id) {
        return crudService.findById(id);
    }
    
    public List<StickerSet> findAll() {
        return crudService.findAll();
    }
    
    public StickerSet save(StickerSet stickerSet) {
        return crudService.save(stickerSet);
    }
    
    /**
     * Удалить стикерсет (soft delete)
     * 
     * @param id ID стикерсета
     * @throws IllegalArgumentException если стикерсет не найден
     */
    @Transactional
    public void deleteById(Long id) {
        crudService.deleteById(id);
    }
    
    /**
     * Получить все стикерсеты с пагинацией и обогащением данных Bot API
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language) {
        return findAllWithPagination(pageRequest, language, null, null, null, false, false, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, boolean shortInfo) {
        return findAllWithPagination(pageRequest, language, null, null, null, false, shortInfo, false);
    }
    
    /**
     * Получить все стикерсеты с пагинацией и обогащением данных Bot API
     * Возвращает только публичные и не заблокированные стикерсеты для галереи
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId) {
        return findAllWithPagination(pageRequest, language, currentUserId, null, null, false, false, false);
    }
    
    /**
     * Получить все стикерсеты с опциональной фильтрацией по type
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId, StickerSetType type) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, null, null, false, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId, StickerSetType type, boolean shortInfo) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, null, null, shortInfo, false);
    }
    
    /**
     * Получить все стикерсеты с дополнительной фильтрацией по userId/isVerified
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             StickerSetType type, Long userId, Boolean isVerified) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, userId, isVerified, false, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             StickerSetType type, Long userId, Boolean isVerified, boolean shortInfo) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, userId, isVerified, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             StickerSetType type, Long userId, Boolean isVerified, boolean shortInfo, boolean preview) {
        LOGGER.debug("📋 Получение стикерсетов с пагинацией: page={}, size={}, language={}, shortInfo={}, preview={}, type={}, userId={}, isVerified={}",
                pageRequest.getPage(), pageRequest.getSize(), language, shortInfo, preview, type, userId, isVerified);
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findPublicNotBlockedFiltered(type, userId, isVerified, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId, shortInfo, preview, false);
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсеты пользователя с пагинацией и обогащением данных Bot API
     * @param userId ID пользователя, чьи стикерсеты запрашиваются
     * @param pageRequest параметры пагинации/сортировки
     * @param categoryKeys фильтр по категориям (может быть null или пустым)
     * @param isVerified показать только верифицированные стикерсеты
     * @param likedOnly показать только стикерсеты, лайкнутые текущим пользователем
     * @param currentUserId ID текущего авторизованного пользователя (может быть null)
     * @param visibilityFilter фильтр видимости (ALL/PUBLIC/PRIVATE)
     * @param type фильтр по типу стикерсета (может быть null)
     * @param shortInfo возвращать только краткую информацию без telegramStickerSetInfo
     * @param preview возвращать только 1 случайный стикер в telegramStickerSetInfo
     * @param language язык для локализации категорий
     */
    public PageResponse<StickerSetDto> findByUserIdWithPagination(Long userId,
                                                                  PageRequest pageRequest,
                                                                  Set<String> categoryKeys,
                                                                  Boolean isVerified,
                                                                  boolean likedOnly,
                                                                  Long currentUserId,
                                                                  com.example.sticker_art_gallery.dto.VisibilityFilter visibilityFilter,
                                                                  StickerSetType type,
                                                                  boolean shortInfo,
                                                                  boolean preview,
                                                                  String language) {
        return findByUserIdWithPagination(userId, pageRequest, categoryKeys, isVerified, likedOnly, 
                                          currentUserId, visibilityFilter, type, shortInfo, preview, language, false);
    }
    
    public PageResponse<StickerSetDto> findByUserIdWithPagination(Long userId,
                                                                  PageRequest pageRequest,
                                                                  Set<String> categoryKeys,
                                                                  Boolean isVerified,
                                                                  boolean likedOnly,
                                                                  Long currentUserId,
                                                                  com.example.sticker_art_gallery.dto.VisibilityFilter visibilityFilter,
                                                                  StickerSetType type,
                                                                  boolean shortInfo,
                                                                  boolean preview,
                                                                  String language,
                                                                  boolean includeBlocked) {
        String lang = normalizeLanguage(language);
        LOGGER.debug("👤 Получение стикерсетов пользователя {} с пагинацией: page={}, size={}, isVerified={}, likedOnly={}, visibilityFilter={}, type={}, shortInfo={}, preview={}, language={}, categoryKeys={}, includeBlocked={}",
                userId, pageRequest.getPage(), pageRequest.getSize(), isVerified, likedOnly, visibilityFilter, type, shortInfo, preview, lang,
                categoryKeys == null ? "null" : String.join(",", categoryKeys), includeBlocked);

        Set<String> normalizedCategoryKeys = (categoryKeys == null || categoryKeys.isEmpty()) ? null : categoryKeys;

        Page<StickerSet> stickerSetsPage = stickerSetRepository.findUserStickerSetsFiltered(
                userId,
                visibilityFilter.name(),
                type,
                isVerified,
                normalizedCategoryKeys,
                likedOnly,
                currentUserId,
                includeBlocked,
                pageRequest.toPageable()
        );

        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), lang, currentUserId, shortInfo, preview, false);

        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить верифицированные стикерсеты владельца (deprecated: authorId => userId + isVerified)
     * @param userId ID владельца (ранее authorId)
     * @param pageRequest параметры пагинации/сортировки
     * @param categoryKeys фильтр по категориям (может быть null или пустым)
     * @param currentUserId ID текущего авторизованного пользователя (может быть null)
     * @param visibilityFilter фильтр видимости (ALL/PUBLIC/PRIVATE)
     * @param type фильтр по типу стикерсета (может быть null)
     * @param shortInfo возвращать только краткую информацию без telegramStickerSetInfo
     * @param preview возвращать только 1 случайный стикер в telegramStickerSetInfo
     * @param language язык для локализации категорий
     */
    public PageResponse<StickerSetDto> findByAuthorIdWithPagination(Long userId,
                                                                    PageRequest pageRequest,
                                                                    Set<String> categoryKeys,
                                                                    Long currentUserId,
                                                                    com.example.sticker_art_gallery.dto.VisibilityFilter visibilityFilter,
                                                                    StickerSetType type,
                                                                    boolean shortInfo,
                                                                    boolean preview,
                                                                    String language) {
        String lang = normalizeLanguage(language);
        LOGGER.debug("✍️ Получение верифицированных стикерсетов владельца {} с пагинацией: page={}, size={}, visibilityFilter={}, type={}, shortInfo={}, preview={}, categoryKeys={}, language={}",
                userId, pageRequest.getPage(), pageRequest.getSize(), visibilityFilter, type, shortInfo, preview,
                categoryKeys == null ? "null" : String.join(",", categoryKeys), lang);

        Set<String> normalizedCategoryKeys = (categoryKeys == null || categoryKeys.isEmpty()) ? null : categoryKeys;

        Page<StickerSet> stickerSetsPage = stickerSetRepository.findVerifiedOwnerStickerSetsFiltered(
                userId,
                visibilityFilter.name(),
                type,
                normalizedCategoryKeys,
                pageRequest.toPageable()
        );

        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), lang, currentUserId, shortInfo, preview, false);

        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсеты по ключам категорий с пагинацией и обогащением данных Bot API
     */
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, null, null, false, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, boolean shortInfo) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, null, null, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, null, false, false);
    }
    
    /**
     * Получить стикерсеты по ключам категорий с опциональной фильтрацией по type
     */
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId, StickerSetType type) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, type, false, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId, StickerSetType type, boolean shortInfo) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, type, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId, StickerSetType type, boolean shortInfo, boolean preview) {
        LOGGER.debug("🏷️ Получение стикерсетов по категориям {} с пагинацией: page={}, size={}, type={}, shortInfo={}, preview={}", 
                String.join(",", categoryKeys), pageRequest.getPage(), pageRequest.getSize(), type, shortInfo, preview);
        Page<StickerSet> stickerSetsPage = type == StickerSetType.OFFICIAL
                ? stickerSetRepository.findByCategoryKeysPublicNotBlockedAndOfficial(categoryKeys, pageRequest.toPageable())
                : stickerSetRepository.findByCategoryKeysPublicAndNotBlocked(categoryKeys, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId, shortInfo, preview, false);
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсеты по категориям с дополнительной фильтрацией по userId/isVerified
     */
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          StickerSetType type, Long userId, Boolean isVerified) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, type, userId, isVerified, false, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          StickerSetType type, Long userId, Boolean isVerified, boolean shortInfo) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, type, userId, isVerified, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          StickerSetType type, Long userId, Boolean isVerified, boolean shortInfo, boolean preview) {
        LOGGER.debug("🏷️ Получение стикерсетов по категориям {} с пагинацией: page={}, size={}, shortInfo={}, preview={}, type={}, userId={}, isVerified={}",
                String.join(",", categoryKeys), pageRequest.getPage(), pageRequest.getSize(), shortInfo, preview, type, userId, isVerified);
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findByCategoryKeysPublicNotBlockedFiltered(categoryKeys, type, userId, isVerified, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId, shortInfo, preview, false);
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить стикерсет по ID с обогащением данных Bot API
     * Если Bot API недоступен, возвращает стикерсет без обогащения
     */
    public StickerSetDto findByIdWithBotApiData(Long id) {
        return findByIdWithBotApiData(id, null, null, false);
    }
    
    /**
     * Получить стикерсет по ID с обогащением данных Bot API и информацией о лайке текущего пользователя
     * Если Bot API недоступен, возвращает стикерсет без обогащения
     * @param id ID стикерсета
     * @param currentUserId ID текущего пользователя (может быть null для неавторизованных пользователей)
     * @return StickerSetDto с полем isLikedByCurrentUser
     */
    public StickerSetDto findByIdWithBotApiData(Long id, Long currentUserId) {
        return findByIdWithBotApiData(id, null, currentUserId, false);
    }
    
    /**
     * Получить стикерсет по ID с учётом языка и лайков пользователя
     */
    public StickerSetDto findByIdWithBotApiData(Long id, String language, Long currentUserId) {
        return findByIdWithBotApiData(id, language, currentUserId, false);
    }
    
    public StickerSetDto findByIdWithBotApiData(Long id, String language, Long currentUserId, boolean shortInfo) {
        LOGGER.debug("🔍 Получение стикерсета по ID {} с данными Bot API (language={}, currentUserId={}, shortInfo={})", id, language, currentUserId, shortInfo);
        
        StickerSet stickerSet = crudService.findById(id);
        if (stickerSet == null) {
            return null;
        }
        
        String lang = normalizeLanguage(language);
        
        // Обогащаем данными Telegram API
        StickerSetDto dto = enrichSingleStickerSetSafelyWithCategories(stickerSet, lang, currentUserId, shortInfo, false, true);
        
        // Обновляем title и stickers_count в БД, если данные Telegram API получены
        // Обновление выполняется только если shortInfo == false (данные Telegram API получены)
        if (!shortInfo && dto != null && dto.getTelegramStickerSetInfo() != null) {
            try {
                // Получаем оригинальные данные из кэша (не отфильтрованные для preview)
                // Кэш уже использован при обогащении, поэтому это не создаст дополнительного запроса
                Object botApiData = telegramBotApiService.getStickerSetInfo(stickerSet.getName());
                enrichmentService.updateTitleAndStickersCount(stickerSet, botApiData);
            } catch (Exception e) {
                LOGGER.warn("⚠️ Ошибка при обновлении title и stickers_count для стикерсета {}: {} - продолжаем выполнение", 
                        id, e.getMessage());
                // Не прерываем выполнение - ошибка обновления не должна влиять на возврат DTO
            }
        }
        
        LOGGER.debug("🔍 Стикерсет ID {}: userId={}, currentUserId={}, state={}, visibility={}, availableActions={}", 
                id, stickerSet.getUserId(), currentUserId, stickerSet.getState(), stickerSet.getVisibility(), 
                dto != null ? dto.getAvailableActions() : "null");
        
        return dto;
    }
    
    /**
     * Получить стикерсет по имени с обогащением данных Bot API
     * Если Bot API недоступен, возвращает стикерсет без обогащения
     */
    public StickerSetDto findByNameWithBotApiData(String name) {
        return findByNameWithBotApiData(name, false);
    }
    
    public StickerSetDto findByNameWithBotApiData(String name, boolean shortInfo) {
        LOGGER.debug("🔍 Получение стикерсета по имени '{}' с данными Bot API (shortInfo={})", name, shortInfo);
        
        StickerSet stickerSet = stickerSetRepository.findByName(name).orElse(null);
        if (stickerSet == null) {
            return null;
        }
        
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, "en", null, shortInfo, false, true);
    }
    
    /**
     * Поиск стикерсетов по частичному совпадению в title или description
     */
    public PageResponse<StickerSetDto> searchStickerSets(String query,
                                                         PageRequest pageRequest,
                                                         Set<String> categoryKeys,
                                                         StickerSetType type,
                                                         Long userId,
                                                         Boolean isVerified,
                                                         Long currentUserId,
                                                         String language,
                                                         boolean shortInfo) {
        return searchStickerSets(query, pageRequest, categoryKeys, type, userId, isVerified, currentUserId, language, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> searchStickerSets(String query,
                                                         PageRequest pageRequest,
                                                         Set<String> categoryKeys,
                                                         StickerSetType type,
                                                         Long userId,
                                                         Boolean isVerified,
                                                         Long currentUserId,
                                                         String language,
                                                         boolean shortInfo,
                                                         boolean preview) {
        String lang = normalizeLanguage(language);
        LOGGER.debug("🔍 Поиск стикерсетов по query='{}': page={}, size={}, type={}, shortInfo={}, preview={}, categoryKeys={}, userId={}, isVerified={}", 
                query, pageRequest.getPage(), pageRequest.getSize(), type, shortInfo, preview,
                categoryKeys == null ? "null" : String.join(",", categoryKeys), userId, isVerified);
        
        Page<StickerSet> stickerSetsPage = stickerSetRepository.searchPublicStickerSets(
                query, categoryKeys, type, userId, isVerified, pageRequest.toPageable());
        
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(
                stickerSetsPage.getContent(), lang, currentUserId, shortInfo, preview, false);
        
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Обновить видимость стикерсета (устаревший метод, используйте publishStickerSet/unpublishStickerSet)
     */
    @Deprecated
    @Transactional
    public StickerSet updateVisibility(Long stickerSetId, Boolean isPublic) {
        if (Boolean.TRUE.equals(isPublic)) {
            return publishStickerSet(stickerSetId);
        } else {
            return unpublishStickerSet(stickerSetId);
        }
    }
    
    /**
     * Опубликовать стикерсет (PRIVATE -> PUBLIC) с начислением ART за первую публикацию
     */
    @Transactional
    public StickerSet publishStickerSet(Long id) {
        return visibilityService.publishStickerSet(id);
    }
    
    /**
     * Сделать стикерсет приватным (PUBLIC -> PRIVATE)
     */
    @Transactional
    public StickerSet unpublishStickerSet(Long id) {
        return visibilityService.unpublishStickerSet(id);
    }
    
    /**
     * Заблокировать стикерсет (только для админа)
     */
    @Transactional
    public StickerSet blockStickerSet(Long stickerSetId, String reason) {
        return visibilityService.blockStickerSet(stickerSetId, reason);
    }
    
    /**
     * Разблокировать стикерсет (только для админа)
     */
    @Transactional
    public StickerSet unblockStickerSet(Long stickerSetId) {
        return visibilityService.unblockStickerSet(stickerSetId);
    }
    
    /**
     * Установить официальный статус стикерсета (только для админа)
     */
    @Transactional
    public StickerSet setOfficial(Long stickerSetId) {
        return visibilityService.setOfficial(stickerSetId);
    }
    
    /**
     * Снять официальный статус стикерсета (только для админа)
     */
    @Transactional
    public StickerSet unsetOfficial(Long stickerSetId) {
        return visibilityService.unsetOfficial(stickerSetId);
    }
    
    
    /**
     * Обновить категории стикерсета
     */
    @Transactional
    public StickerSet updateCategories(Long stickerSetId, Set<String> categoryKeys) {
        return crudService.updateCategories(stickerSetId, categoryKeys);
    }
    
    public List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId, boolean shortInfo) {
        return enrichWithBotApiDataAndCategories(stickerSets, language, currentUserId, shortInfo, false, false);
    }
    
    public List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId, boolean shortInfo, boolean preview) {
        return enrichWithBotApiDataAndCategories(stickerSets, language, currentUserId, shortInfo, preview, false);
    }
    
    public List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId, boolean shortInfo, boolean preview, boolean includeAvailableActions) {
        return enrichmentService.enrichWithBotApiDataAndCategories(stickerSets, language, currentUserId, shortInfo, preview, includeAvailableActions);
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API и категориями (безопасно)
     * @param preview если true, возвращает только 1 случайный стикер вместо полного списка
     * @param includeAvailableActions если true, вычисляет доступные действия для стикерсета
     */
    private StickerSetDto enrichSingleStickerSetSafelyWithCategories(StickerSet stickerSet, String language, Long currentUserId, boolean shortInfo, boolean preview, boolean includeAvailableActions) {
        return enrichmentService.enrichSingleStickerSetSafelyWithCategories(stickerSet, language, currentUserId, shortInfo, preview, includeAvailableActions);
    }
    public PageResponse<StickerSetDto> searchStickerSets(String query,
                                                          PageRequest pageRequest,
                                                          Set<String> categoryKeys,
                                                          StickerSetType type,
                                                          Long userId,
                                                          Boolean isVerified,
                                                          boolean likedOnly,
                                                          Long currentUserId,
                                                          String language,
                                                          boolean shortInfo) {
        return searchStickerSets(query, pageRequest, categoryKeys, type, userId, isVerified, likedOnly, currentUserId, language, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> searchStickerSets(String query,
                                                          PageRequest pageRequest,
                                                          Set<String> categoryKeys,
                                                          StickerSetType type,
                                                          Long userId,
                                                          Boolean isVerified,
                                                          boolean likedOnly,
                                                          Long currentUserId,
                                                          String language,
                                                          boolean shortInfo,
                                                          boolean preview) {
        String lang = normalizeLanguage(language);
        LOGGER.debug("🔍 Поиск стикерсетов по запросу '{}': page={}, size={}, type={}, likedOnly={}, shortInfo={}, preview={}",
                query, pageRequest.getPage(), pageRequest.getSize(), type, likedOnly, shortInfo, preview);
        
        if (likedOnly && currentUserId != null && likeService != null) {
            // Поиск среди лайкнутых через LikeService
            LOGGER.debug("🔍 Поиск среди лайкнутых стикерсетов пользователя {}", currentUserId);
            return likeService.searchLikedStickerSets(currentUserId, query, categoryKeys, pageRequest, lang, shortInfo, preview);
        }
        
        // Поиск среди публичных
        Page<StickerSet> stickerSetsPage = stickerSetRepository.searchPublicStickerSets(
                query, categoryKeys, type, userId, isVerified, pageRequest.toPageable());
        
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(
                stickerSetsPage.getContent(), lang, currentUserId, shortInfo, preview, false);
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
    
    /**
     * Получить случайный стикерсет, который пользователь еще не лайкал и не дизлайкал
     * @param userId ID пользователя
     * @param language язык для локализации
     * @param shortInfo если true, не обогащать данными из Telegram Bot API
     * @return случайный стикерсет или null, если нет доступных стикерсетов
     */
    public StickerSetDto findRandomStickerSetNotRatedByUser(Long userId, String language, boolean shortInfo) {
        LOGGER.debug("🎲 Поиск случайного стикерсета для пользователя {} (shortInfo={})", userId, shortInfo);
        
        Optional<StickerSet> randomStickerSet = stickerSetRepository.findRandomStickerSetNotRatedByUser(userId);
        
        if (randomStickerSet.isEmpty()) {
            LOGGER.debug("⚠️ Не найдено стикерсетов, которые пользователь {} еще не оценивал", userId);
            return null;
        }
        
        StickerSet stickerSet = randomStickerSet.get();
        LOGGER.debug("✅ Найден случайный стикерсет: {} (id={})", stickerSet.getName(), stickerSet.getId());
        
        // Обогащаем данными из Bot API если нужно
        if (shortInfo) {
            String lang = normalizeLanguage(language);
            return StickerSetDto.fromEntity(stickerSet, lang, userId);
        } else {
            return findByIdWithBotApiData(stickerSet.getId(), language, userId, false);
        }
    }
    
    /**
     * Получить батч случайных стикерсетов, которые пользователь еще не лайкал и не дизлайкал
     * @param userId ID пользователя
     * @param pageRequest параметры пагинации
     * @param language язык для локализации
     * @param shortInfo если true, не обогащать данными из Telegram Bot API
     * @param preview если true, возвращает только 1 случайный стикер вместо полного списка
     * @return страница случайных стикерсетов
     */
    public PageResponse<StickerSetDto> findRandomStickerSetsNotRatedByUser(
            Long userId, 
            PageRequest pageRequest, 
            String language, 
            boolean shortInfo, 
            boolean preview) {
        LOGGER.debug("🎲 Поиск батча случайных стикерсетов для пользователя {}: page={}, size={}, shortInfo={}, preview={}", 
                userId, pageRequest.getPage(), pageRequest.getSize(), shortInfo, preview);
        
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findRandomStickerSetsNotRatedByUser(
                userId, pageRequest.toPageable());
        
        String lang = normalizeLanguage(language);
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(
                stickerSetsPage.getContent(), lang, userId, shortInfo, preview, false);
        
        LOGGER.debug("✅ Найдено {} случайных стикерсетов для пользователя {} на странице {} из {}", 
                enrichedDtos.size(), userId, stickerSetsPage.getNumber() + 1, stickerSetsPage.getTotalPages());
        
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
} 