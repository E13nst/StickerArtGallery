package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.category.Category;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.model.profile.ArtTransactionRepository;
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
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;

@Service
public class StickerSetService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetService.class);
    private final StickerSetRepository stickerSetRepository;
    private final TelegramBotApiService telegramBotApiService;
    private final CategoryService categoryService;
    private final ArtRewardService artRewardService;
    private final ArtTransactionRepository artTransactionRepository;
    private LikeService likeService; // Lazy injection to avoid circular dependency
    
    @Autowired
    public StickerSetService(StickerSetRepository stickerSetRepository,
                             TelegramBotApiService telegramBotApiService,
                             CategoryService categoryService,
                             ArtRewardService artRewardService,
                             ArtTransactionRepository artTransactionRepository) {
        this.stickerSetRepository = stickerSetRepository;
        this.telegramBotApiService = telegramBotApiService;
        this.categoryService = categoryService;
        this.artRewardService = artRewardService;
        this.artTransactionRepository = artTransactionRepository;
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
        
        return createStickerSetForUser(createDto, userId, lang, null);
    }

    /**
     * Создает стикерсет от имени конкретного пользователя (используется межсервисным API).
     * Устанавливает visibility = PRIVATE по умолчанию для internal API.
     */
    public StickerSet createStickerSetForUser(CreateStickerSetDto createDto, Long userId, String language, Long authorId) {
        String lang = normalizeLanguage(language);
        if (userId == null) {
            throw new IllegalArgumentException(localize(
                    lang,
                    "Требуется ID пользователя для создания стикерсета",
                    "User ID is required to create a stickerset"
            ));
        }
        LOGGER.info("➕ Создание стикерсета для пользователя {} (authorId={}): {}", userId, authorId, createDto.getName());
        
        // Устанавливаем visibility = PRIVATE по умолчанию для internal API
        if (createDto.getVisibility() == null) {
            // Проверяем обратную совместимость через isPublic
            if (createDto.getIsPublic() != null) {
                createDto.setVisibility(createDto.getIsPublic() ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE);
            } else {
                createDto.setVisibility(StickerSetVisibility.PRIVATE);
            }
        }
        
        return createStickerSetValidated(createDto, userId, lang, authorId);
    }

    private StickerSet createStickerSetValidated(CreateStickerSetDto createDto, Long userId, String lang, Long authorId) {
        // Нормализуем имя стикерсета
        createDto.normalizeName();
        String stickerSetName = createDto.getName();

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
                return restoreAndUpdateStickerSet(existing, createDto, userId, lang, authorId);
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
        return createStickerSetInternal(userId, title, stickerSetName, createDto.getDescription(), createDto.getVisibility(), categories, authorId, false);
    }
    
    /**
     * Восстановление и обновление удаленного стикерсета
     */
    private StickerSet restoreAndUpdateStickerSet(StickerSet existing, CreateStickerSetDto createDto, 
                                                   Long userId, String lang, Long authorId) {
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
        if (authorId != null) {
            existing.setAuthorId(authorId);
        }
        
        // 4. Обновляем title если указан
        if (createDto.getTitle() != null && !createDto.getTitle().trim().isEmpty()) {
            existing.setTitle(createDto.getTitle());
        } else {
            String title = telegramBotApiService.extractTitleFromStickerSetInfo(telegramStickerSetInfo);
            if (title != null && !title.trim().isEmpty()) {
                existing.setTitle(title);
            }
        }
        
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
     */
    private StickerSet createStickerSetInternal(Long userId,
                                               String title,
                                               String name,
                                               String description,
                                               StickerSetVisibility visibility,
                                               List<Category> categories,
                                               Long authorId,
                                               boolean isRestored) {
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
        
        if (authorId != null) {
            stickerSet.setAuthorId(authorId);
        }
        
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
    
    /**
     * Проверяет, является ли текущий пользователь администратором
     */
    private boolean isCurrentUserAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при проверке роли администратора: {}", e.getMessage());
            return false;
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
     * Получить все стикерсеты с пагинацией и обогащением данных Bot API
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language) {
        return findAllWithPagination(pageRequest, language, null, null, null, false, null, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, boolean shortInfo) {
        return findAllWithPagination(pageRequest, language, null, null, null, false, null, shortInfo, false);
    }
    
    /**
     * Получить все стикерсеты с пагинацией и обогащением данных Bot API
     * Возвращает только публичные и не заблокированные стикерсеты для галереи
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId) {
        return findAllWithPagination(pageRequest, language, currentUserId, null, null, false, null, false, false);
    }
    
    /**
     * Получить все стикерсеты с опциональной фильтрацией по type
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId, StickerSetType type) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, null, false, null, false, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId, StickerSetType type, boolean shortInfo) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, null, false, null, shortInfo, false);
    }
    
    /**
     * Получить все стикерсеты с дополнительной фильтрацией по автору
     */
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             StickerSetType type, Long authorId, boolean hasAuthorOnly) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, authorId, hasAuthorOnly, null, false, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             StickerSetType type, Long authorId, boolean hasAuthorOnly, boolean shortInfo) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, authorId, hasAuthorOnly, null, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             StickerSetType type, Long authorId, boolean hasAuthorOnly, Long userId, boolean shortInfo) {
        return findAllWithPagination(pageRequest, language, currentUserId, type, authorId, hasAuthorOnly, userId, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findAllWithPagination(PageRequest pageRequest, String language, Long currentUserId,
                                                             StickerSetType type, Long authorId, boolean hasAuthorOnly, Long userId, boolean shortInfo, boolean preview) {
        LOGGER.debug("📋 Получение стикерсетов{} с пагинацией: page={}, size={}, language={}, shortInfo={}, preview={}, type={}, userId={}",
                authorId != null ? (" автора=" + authorId) : (hasAuthorOnly ? " (только с автором)" : ""),
                pageRequest.getPage(), pageRequest.getSize(), language, shortInfo, preview, type, userId);
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findPublicNotBlockedFiltered(type, authorId, hasAuthorOnly, userId, pageRequest.toPageable());
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(stickerSetsPage.getContent(), language, currentUserId, shortInfo, preview, false);
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
     * @param visibilityFilter фильтр видимости (ALL/PUBLIC/PRIVATE)
     * @param type фильтр по типу стикерсета (может быть null)
     * @param shortInfo возвращать только краткую информацию без telegramStickerSetInfo
     * @param preview возвращать только 1 случайный стикер в telegramStickerSetInfo
     * @param language язык для локализации категорий
     */
    public PageResponse<StickerSetDto> findByUserIdWithPagination(Long userId,
                                                                  PageRequest pageRequest,
                                                                  Set<String> categoryKeys,
                                                                  boolean hasAuthorOnly,
                                                                  boolean likedOnly,
                                                                  Long currentUserId,
                                                                  com.example.sticker_art_gallery.dto.VisibilityFilter visibilityFilter,
                                                                  StickerSetType type,
                                                                  boolean shortInfo,
                                                                  boolean preview,
                                                                  String language) {
        return findByUserIdWithPagination(userId, pageRequest, categoryKeys, hasAuthorOnly, likedOnly, 
                                          currentUserId, visibilityFilter, type, shortInfo, preview, language, false);
    }
    
    public PageResponse<StickerSetDto> findByUserIdWithPagination(Long userId,
                                                                  PageRequest pageRequest,
                                                                  Set<String> categoryKeys,
                                                                  boolean hasAuthorOnly,
                                                                  boolean likedOnly,
                                                                  Long currentUserId,
                                                                  com.example.sticker_art_gallery.dto.VisibilityFilter visibilityFilter,
                                                                  StickerSetType type,
                                                                  boolean shortInfo,
                                                                  boolean preview,
                                                                  String language,
                                                                  boolean includeBlocked) {
        String lang = normalizeLanguage(language);
        LOGGER.debug("👤 Получение стикерсетов пользователя {} с пагинацией: page={}, size={}, hasAuthorOnly={}, likedOnly={}, visibilityFilter={}, type={}, shortInfo={}, preview={}, language={}, categoryKeys={}, includeBlocked={}",
                userId, pageRequest.getPage(), pageRequest.getSize(), hasAuthorOnly, likedOnly, visibilityFilter, type, shortInfo, preview, lang,
                categoryKeys == null ? "null" : String.join(",", categoryKeys), includeBlocked);

        Set<String> normalizedCategoryKeys = (categoryKeys == null || categoryKeys.isEmpty()) ? null : categoryKeys;

        Page<StickerSet> stickerSetsPage = stickerSetRepository.findUserStickerSetsFiltered(
                userId,
                visibilityFilter.name(),
                type,
                hasAuthorOnly,
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
     * Получить авторские стикерсеты с пагинацией и обогащением данных
     * @param authorId ID автора
     * @param pageRequest параметры пагинации/сортировки
     * @param categoryKeys фильтр по категориям (может быть null или пустым)
     * @param currentUserId ID текущего авторизованного пользователя (может быть null)
     * @param visibilityFilter фильтр видимости (ALL/PUBLIC/PRIVATE)
     * @param type фильтр по типу стикерсета (может быть null)
     * @param shortInfo возвращать только краткую информацию без telegramStickerSetInfo
     * @param preview возвращать только 1 случайный стикер в telegramStickerSetInfo
     * @param language язык для локализации категорий
     */
    public PageResponse<StickerSetDto> findByAuthorIdWithPagination(Long authorId,
                                                                    PageRequest pageRequest,
                                                                    Set<String> categoryKeys,
                                                                    Long currentUserId,
                                                                    com.example.sticker_art_gallery.dto.VisibilityFilter visibilityFilter,
                                                                    StickerSetType type,
                                                                    boolean shortInfo,
                                                                    boolean preview,
                                                                    String language) {
        String lang = normalizeLanguage(language);
        LOGGER.debug("✍️ Получение авторских стикерсетов {} с пагинацией: page={}, size={}, visibilityFilter={}, type={}, shortInfo={}, preview={}, categoryKeys={}, language={}",
                authorId, pageRequest.getPage(), pageRequest.getSize(), visibilityFilter, type, shortInfo, preview,
                categoryKeys == null ? "null" : String.join(",", categoryKeys), lang);

        Set<String> normalizedCategoryKeys = (categoryKeys == null || categoryKeys.isEmpty()) ? null : categoryKeys;

        Page<StickerSet> stickerSetsPage = stickerSetRepository.findAuthorStickerSetsFiltered(
                authorId,
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
     * Получить стикерсеты по категориям с дополнительной фильтрацией по автору
     */
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          StickerSetType type, Long authorId, boolean hasAuthorOnly) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, type, authorId, hasAuthorOnly, null, false, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          StickerSetType type, Long authorId, boolean hasAuthorOnly, boolean shortInfo) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, type, authorId, hasAuthorOnly, null, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          StickerSetType type, Long authorId, boolean hasAuthorOnly, Long userId, boolean shortInfo) {
        return findByCategoryKeys(categoryKeys, pageRequest, language, currentUserId, type, authorId, hasAuthorOnly, userId, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> findByCategoryKeys(String[] categoryKeys, PageRequest pageRequest, String language, Long currentUserId,
                                                          StickerSetType type, Long authorId, boolean hasAuthorOnly, Long userId, boolean shortInfo, boolean preview) {
        LOGGER.debug("🏷️ Получение стикерсетов по категориям {}{} с пагинацией: page={}, size={}, shortInfo={}, preview={}, type={}, userId={}",
                String.join(",", categoryKeys),
                authorId != null ? (" автора=" + authorId) : (hasAuthorOnly ? " (только с автором)" : ""),
                pageRequest.getPage(), pageRequest.getSize(), shortInfo, preview, type, userId);
        Page<StickerSet> stickerSetsPage = stickerSetRepository.findByCategoryKeysPublicNotBlockedFiltered(categoryKeys, type, authorId, hasAuthorOnly, userId, pageRequest.toPageable());
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
        
        StickerSet stickerSet = stickerSetRepository.findById(id).orElse(null);
        if (stickerSet == null) {
            return null;
        }
        
        String lang = normalizeLanguage(language);
        StickerSetDto dto = enrichSingleStickerSetSafelyWithCategories(stickerSet, lang, currentUserId, shortInfo, false, true);
        
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
                                                         Long authorId,
                                                         boolean hasAuthorOnly,
                                                         Long userId,
                                                         Long currentUserId,
                                                         String language,
                                                         boolean shortInfo) {
        return searchStickerSets(query, pageRequest, categoryKeys, type, authorId, hasAuthorOnly, userId, currentUserId, language, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> searchStickerSets(String query,
                                                         PageRequest pageRequest,
                                                         Set<String> categoryKeys,
                                                         StickerSetType type,
                                                         Long authorId,
                                                         boolean hasAuthorOnly,
                                                         Long userId,
                                                         Long currentUserId,
                                                         String language,
                                                         boolean shortInfo,
                                                         boolean preview) {
        String lang = normalizeLanguage(language);
        LOGGER.debug("🔍 Поиск стикерсетов по query='{}': page={}, size={}, type={}, shortInfo={}, preview={}, categoryKeys={}", 
                query, pageRequest.getPage(), pageRequest.getSize(), type, shortInfo, preview,
                categoryKeys == null ? "null" : String.join(",", categoryKeys));
        
        Page<StickerSet> stickerSetsPage = stickerSetRepository.searchPublicStickerSets(
                query, categoryKeys, type, authorId, hasAuthorOnly, userId, pageRequest.toPageable());
        
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
        StickerSet stickerSet = findById(id);
        if (stickerSet == null) {
            throw new IllegalArgumentException("Стикерсет не найден");
        }
        
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
        StickerSet stickerSet = findById(id);
        if (stickerSet == null) {
            throw new IllegalArgumentException("Стикерсет не найден");
        }
        
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
    private List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId) {
        return enrichWithBotApiDataAndCategories(stickerSets, language, currentUserId, false, false, false);
    }
    
    public List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId, boolean shortInfo) {
        return enrichWithBotApiDataAndCategories(stickerSets, language, currentUserId, shortInfo, false, false);
    }
    
    public List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId, boolean shortInfo, boolean preview) {
        return enrichWithBotApiDataAndCategories(stickerSets, language, currentUserId, shortInfo, preview, false);
    }
    
    public List<StickerSetDto> enrichWithBotApiDataAndCategories(List<StickerSet> stickerSets, String language, Long currentUserId, boolean shortInfo, boolean preview, boolean includeAvailableActions) {
        if (stickerSets.isEmpty()) {
            return List.of();
        }
        
        LOGGER.debug("🚀 Обогащение {} стикерсетов данными Bot API и категориями (последовательно, shortInfo={}, preview={}, includeAvailableActions={})", stickerSets.size(), shortInfo, preview, includeAvailableActions);
        
        // Обрабатываем последовательно, чтобы избежать проблем с Hibernate Session
        List<StickerSetDto> result = stickerSets.stream()
                .map(stickerSet -> enrichSingleStickerSetSafelyWithCategories(stickerSet, language, currentUserId, shortInfo, preview, includeAvailableActions))
                .collect(Collectors.toList());
        
        LOGGER.debug("✅ Обогащение завершено для {} стикерсетов", result.size());
        return result;
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API и категориями (безопасно)
     */
    private StickerSetDto enrichSingleStickerSetSafelyWithCategories(StickerSet stickerSet, String language) {
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, language, null, false, false, true);
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API и категориями (безопасно)
     */
    private StickerSetDto enrichSingleStickerSetSafelyWithCategories(StickerSet stickerSet, String language, Long currentUserId) {
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, language, currentUserId, false, false, true);
    }
    
    /**
     * Фильтрует стикеры в telegramStickerSetInfo для режима превью.
     * Оставляет только 1 случайный стикер из полного списка.
     */
    private Object filterStickersForPreview(Object telegramStickerSetInfo) {
        if (telegramStickerSetInfo instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> infoMap = new java.util.LinkedHashMap<>((java.util.Map<String, Object>) telegramStickerSetInfo);
            
            Object stickersObj = infoMap.get("stickers");
            if (stickersObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> stickers = (java.util.List<Object>) stickersObj;
                
                if (stickers.size() > 1) {
                    // Выбираем 1 случайный стикер
                    java.util.List<Object> shuffled = new java.util.ArrayList<>(stickers);
                    java.util.Collections.shuffle(shuffled);
                    infoMap.put("stickers", shuffled.subList(0, 1));
                    LOGGER.debug("🎲 Фильтрация стикеров для превью: {} -> 1 случайный", stickers.size());
                }
            }
            return infoMap;
        }
        return telegramStickerSetInfo;
    }
    
    /**
     * Обогащает один стикерсет данными из Bot API и категориями (безопасно)
     * @param preview если true, возвращает только 1 случайный стикер вместо полного списка
     * @param includeAvailableActions если true, вычисляет доступные действия для стикерсета
     */
    private StickerSetDto enrichSingleStickerSetSafelyWithCategories(StickerSet stickerSet, String language, Long currentUserId, boolean shortInfo, boolean preview, boolean includeAvailableActions) {
        boolean isAdmin = isCurrentUserAdmin();
        LOGGER.debug("🔍 Обогащение стикерсета {}: currentUserId={}, stickerSetUserId={}, isAdmin={}, preview={}, includeAvailableActions={}", 
                stickerSet.getId(), currentUserId, stickerSet.getUserId(), isAdmin, preview, includeAvailableActions);
        StickerSetDto dto = StickerSetDto.fromEntity(stickerSet, language, currentUserId, isAdmin, includeAvailableActions);
        
        if (dto == null) {
            LOGGER.warn("⚠️ Не удалось создать DTO для стикерсета {}", stickerSet.getId());
            return null;
        }
        
        LOGGER.debug("🔍 Результат обогащения стикерсета {}: availableActions={}", 
                stickerSet.getId(), dto.getAvailableActions());
        
        if (shortInfo) {
            dto.setTelegramStickerSetInfo(null);
            return dto;
        }
        
        try {
            Object botApiData = telegramBotApiService.getStickerSetInfo(stickerSet.getName());
            
            // Применяем фильтрацию для режима превью
            if (preview && botApiData != null) {
                botApiData = filterStickersForPreview(botApiData);
            }
            
            dto.setTelegramStickerSetInfo(botApiData);
            LOGGER.debug("✅ Стикерсет '{}' обогащен данными Bot API (preview={})", stickerSet.getName(), preview);
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
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, "en", null, false, false, true);
    }
    
    private StickerSetDto enrichSingleStickerSetSafely(StickerSet stickerSet, boolean shortInfo) {
        return enrichSingleStickerSetSafelyWithCategories(stickerSet, "en", null, shortInfo, false, true);
    }
    public PageResponse<StickerSetDto> searchStickerSets(String query,
                                                          PageRequest pageRequest,
                                                          Set<String> categoryKeys,
                                                          StickerSetType type,
                                                          Long authorId,
                                                          boolean hasAuthorOnly,
                                                          Long userId,
                                                          boolean likedOnly,
                                                          Long currentUserId,
                                                          String language,
                                                          boolean shortInfo) {
        return searchStickerSets(query, pageRequest, categoryKeys, type, authorId, hasAuthorOnly, userId, likedOnly, currentUserId, language, shortInfo, false);
    }
    
    public PageResponse<StickerSetDto> searchStickerSets(String query,
                                                          PageRequest pageRequest,
                                                          Set<String> categoryKeys,
                                                          StickerSetType type,
                                                          Long authorId,
                                                          boolean hasAuthorOnly,
                                                          Long userId,
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
                query, categoryKeys, type, authorId, hasAuthorOnly, userId, pageRequest.toPageable());
        
        List<StickerSetDto> enrichedDtos = enrichWithBotApiDataAndCategories(
                stickerSetsPage.getContent(), lang, currentUserId, shortInfo, preview, false);
        return PageResponse.of(stickerSetsPage, enrichedDtos);
    }
} 