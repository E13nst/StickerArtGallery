package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetModerationStatsDto;
import com.example.sticker_art_gallery.dto.generation.AdminUserPresetModerationPatchDto;
import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetCategoryDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetFieldDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetSystemFields;
import com.example.sticker_art_gallery.model.generation.StylePresetCategoryEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetRemoveBackgroundMode;
import com.example.sticker_art_gallery.model.generation.StylePresetUiMode;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.StylePresetCategoryRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.sticker_art_gallery.dto.generation.StylePresetPromptInputDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetReferenceInputDto;
import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.repository.generation.UserPresetLikeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StylePresetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetService.class);

    private static final String GENERAL_CATEGORY_CODE = "general";

    private final StylePresetRepository presetRepository;
    private final StylePresetCategoryRepository categoryRepository;
    private final UserProfileService userProfileService;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;
    private final StylePresetPromptComposer presetPromptComposer;
    private final UserPresetLikeRepository userPresetLikeRepository;
    private final AppConfig appConfig;

    @Autowired
    public StylePresetService(
            StylePresetRepository presetRepository,
            StylePresetCategoryRepository categoryRepository,
            UserProfileService userProfileService,
            ImageStorageService imageStorageService,
            ObjectMapper objectMapper,
            StylePresetPromptComposer presetPromptComposer,
            UserPresetLikeRepository userPresetLikeRepository,
            AppConfig appConfig) {
        this.presetRepository = presetRepository;
        this.categoryRepository = categoryRepository;
        this.userProfileService = userProfileService;
        this.imageStorageService = imageStorageService;
        this.objectMapper = objectMapper;
        this.presetPromptComposer = presetPromptComposer;
        this.userPresetLikeRepository = userPresetLikeRepository;
        this.appConfig = appConfig;
    }

    @Transactional(readOnly = true)
    public List<StylePresetDto> getAvailablePresets(Long userId, boolean includeUi) {
        return getAvailablePresets(userId, includeUi, false);
    }

    /**
     * @param suppressConsumerPrivacy если true (например админ), в DTO не скрываются авторский промпт и флаги потребителя каталога
     */
    @Transactional(readOnly = true)
    public List<StylePresetDto> getAvailablePresets(Long userId, boolean includeUi, boolean suppressConsumerPrivacy) {
        LOGGER.info("Getting available presets for user {} includeUi={} suppressConsumerPrivacy={}",
                userId, includeUi, suppressConsumerPrivacy);
        List<StylePresetEntity> presets = includeUi
                ? presetRepository.findAvailableForUserWithPreview(userId)
                : presetRepository.findAvailableForUser(userId);
        Long viewer = suppressConsumerPrivacy ? null : userId;
        return presets.stream()
                .map(p -> toDto(p, includeUi, viewer))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StylePresetDto> getAllGlobalPresets() {
        return presetRepository.findAllGlobal().stream()
                .map(p -> toDto(p, true, null))
                .collect(Collectors.toList());
    }

    /**
     * Список пользовательских (не глобальных) пресетов для админ-модерации.
     *
     * @param status фильтр по статусу модерации; {@code null} — все
     */
    @Transactional(readOnly = true)
    public List<StylePresetDto> listUserPresetsForAdmin(PresetModerationStatus status) {
        return presetRepository.findUserPresetsForAdmin(status).stream()
                .map(p -> toDto(p, true, null))
                .collect(Collectors.toList());
    }

    /**
     * Агрегированная статистика по пользовательским пресетам и сохранениям.
     */
    @Transactional(readOnly = true)
    public StylePresetModerationStatsDto getModerationStats() {
        StylePresetModerationStatsDto dto = new StylePresetModerationStatsDto();
        dto.setTotalUserPresets(presetRepository.countByIsGlobalFalse());
        dto.setUserPresetsWithReference(presetRepository.countUserPresetsWithReferenceImage());
        dto.setTotalUserPresetLikes(userPresetLikeRepository.count());

        dto.setDraftCount(0L);
        dto.setPendingModerationCount(0L);
        dto.setApprovedCount(0L);
        dto.setRejectedCount(0L);

        for (Object[] row : presetRepository.countUserPresetsGroupedByModerationStatus()) {
            PresetModerationStatus st = (PresetModerationStatus) row[0];
            long cnt = (Long) row[1];
            if (st == null) {
                continue;
            }
            switch (st) {
                case DRAFT -> dto.setDraftCount(cnt);
                case PENDING_MODERATION -> dto.setPendingModerationCount(cnt);
                case APPROVED -> dto.setApprovedCount(cnt);
                case REJECTED -> dto.setRejectedCount(cnt);
            }
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public List<StylePresetDto> getUserPresets(Long userId) {
        return presetRepository.findByOwnerUserId(userId).stream()
                .map(p -> toDto(p, true, userId))
                .collect(Collectors.toList());
    }

    /**
     * Несохранённый пресет для pipeline генерации по шаблону «свой стиль» (без строки в {@code style_presets}).
     * Предварительно проверьте контракт через {@link #validateBlueprintPresetUiContract(CreateStylePresetRequest)},
     * если в теле могут быть строки {@code preset_ref} в {@code fields} (метаданные слота из шаблона).
     */
    @Transactional(readOnly = true)
    public StylePresetEntity materializeTransientPresetForGeneration(CreateStylePresetRequest request) {
        StylePresetEntity preset = new StylePresetEntity();
        applyUiFields(preset, request);
        preset.setPromptSuffix(request.getPromptSuffix());
        preset.setCode(request.getCode());
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setIsEnabled(true);
        preset.setIsGlobal(false);
        preset.setOwner(null);
        preset.setReferenceImage(null);
        preset.setPreviewImage(null);
        preset.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        preset.setCategory(resolveCategory(request.getCategoryId()));
        return preset;
    }

    @Transactional
    public StylePresetDto createGlobalPreset(CreateStylePresetRequest request) {
        if (containsPresetRefUiRow(request.getFields())) {
            throw new IllegalArgumentException("Global preset cannot include preset_ref in fields; use admin reference upload");
        }
        LOGGER.info("Creating global preset: code={}", request.getCode());
        presetRepository.findByCodeAndIsGlobalTrue(request.getCode())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Global preset with code '" + request.getCode() + "' already exists");
                });
        StylePresetEntity preset = new StylePresetEntity();
        preset.setCode(request.getCode());
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setPromptSuffix(request.getPromptSuffix());
        applyUiFields(preset, request);
        preset.setIsGlobal(true);
        preset.setOwner(null);
        preset.setIsEnabled(true);
        preset.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        preset.setCategory(resolveCategory(request.getCategoryId()));
        preset = presetRepository.save(preset);
        LOGGER.info("Created global preset: id={}, code={}", preset.getId(), preset.getCode());
        return toDto(preset, true, null);
    }

    @Transactional
    public StylePresetDto createUserPreset(Long userId, CreateStylePresetRequest request) {
        String normalizedCode = request.getCode() != null ? request.getCode().trim() : "";
        if (normalizedCode.isEmpty()) {
            throw new IllegalArgumentException("Код пресета не может быть пустым");
        }
        LOGGER.info("Creating user preset for user {}: code={}", userId, normalizedCode);

        Optional<StylePresetEntity> existing = presetRepository.findByCodeAndOwner_UserId(normalizedCode, userId);
        if (existing.isPresent()) {
            StylePresetEntity preset = existing.get();
            LOGGER.info("User preset create-or-get: userId={}, code={}, presetId={} (already exists)",
                    userId, normalizedCode, preset.getId());
            return toDto(preset, true, userId);
        }

        UserProfileEntity profile = userProfileService.getOrCreateDefaultForUpdate(userId);
        StylePresetEntity preset = new StylePresetEntity();
        preset.setCode(normalizedCode);
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setPromptSuffix(request.getPromptSuffix());
        applyUiFields(preset, request);
        preset.setIsGlobal(false);
        preset.setOwner(profile);
        preset.setIsEnabled(true);
        preset.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        preset.setCategory(resolveCategory(request.getCategoryId()));
        try {
            preset = presetRepository.save(preset);
        } catch (DataIntegrityViolationException e) {
            // Параллельные POST с тем же owner+code: unique (code, owner_id)
            return presetRepository.findByCodeAndOwner_UserId(normalizedCode, userId)
                    .map(p -> {
                        LOGGER.info("User preset create-or-get after race: userId={}, code={}, presetId={}",
                                userId, normalizedCode, p.getId());
                        return toDto(p, true, userId);
                    })
                    .orElseThrow(() -> new IllegalStateException(
                            "Конфликт уникальности кода пресета; запись не найдена после повторной выборки", e));
        }
        return toDto(preset, true, userId);
    }

    @Transactional
    public StylePresetDto updatePreset(Long presetId, Long userId, CreateStylePresetRequest request, boolean isAdmin) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (!isAdmin && (Boolean.TRUE.equals(preset.getIsGlobal())
                || preset.getOwner() == null
                || !preset.getOwner().getUserId().equals(userId))) {
            throw new IllegalArgumentException("Access denied: preset is not accessible for user");
        }
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setPromptSuffix(request.getPromptSuffix());
        applyUiFields(preset, request);
        if (request.getSortOrder() != null) {
            preset.setSortOrder(request.getSortOrder());
        }
        if (request.getCategoryId() != null) {
            preset.setCategory(resolveCategory(request.getCategoryId()));
        }
        preset = presetRepository.save(preset);
        return toDto(preset, true, isAdmin ? null : userId);
    }

    @Transactional
    public void deletePreset(Long presetId, Long userId, boolean isAdmin) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (!isAdmin && (Boolean.TRUE.equals(preset.getIsGlobal())
                || preset.getOwner() == null
                || !preset.getOwner().getUserId().equals(userId))) {
            throw new IllegalArgumentException("Access denied: preset is not accessible for user");
        }
        if (!isAdmin && !Boolean.TRUE.equals(preset.getIsGlobal())
                && preset.getModerationStatus() == PresetModerationStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Cannot delete an approved user preset; unpublish from catalog first or contact support");
        }
        if (preset.getPreviewImage() != null) {
            imageStorageService.deleteById(preset.getPreviewImage().getId());
        }
        if (preset.getReferenceImage() != null) {
            imageStorageService.deleteById(preset.getReferenceImage().getId());
        }
        presetRepository.delete(preset);
    }

    @Transactional
    public StylePresetDto togglePresetEnabled(Long presetId, boolean enabled) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        preset.setIsEnabled(enabled);
        preset = presetRepository.save(preset);
        return toDto(preset, true, null);
    }

    @Transactional
    public StylePresetDto uploadPreviewForGlobal(Long presetId, MultipartFile file) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (!Boolean.TRUE.equals(preset.getIsGlobal())) {
            throw new IllegalArgumentException("Only global presets use admin preview upload in this version");
        }
        return persistPresetPreviewUpload(preset, file);
    }

    /**
     * Admin: заменить превью-картинку пользовательского пресета (модерация и правка каталога).
     */
    @Transactional
    public StylePresetDto uploadPreviewForUserPresetAsAdmin(Long presetId, MultipartFile file) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (Boolean.TRUE.equals(preset.getIsGlobal())) {
            throw new IllegalArgumentException("Для глобального пресета используйте POST /api/generation/style-presets/{id}/preview");
        }
        if (preset.getOwner() == null) {
            throw new IllegalArgumentException("Пресет без владельца");
        }
        return persistPresetPreviewUpload(preset, file);
    }

    @Transactional
    public StylePresetDto adminPatchUserPresetForModeration(Long presetId, AdminUserPresetModerationPatchDto body) {
        if (body == null) {
            throw new IllegalArgumentException("body не может быть null");
        }
        body.validatePresent();
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (Boolean.TRUE.equals(preset.getIsGlobal())) {
            throw new IllegalArgumentException("Только пользовательские пресеты");
        }
        if (preset.getOwner() == null) {
            throw new IllegalArgumentException("Пресет без владельца");
        }
        String newName = body.trimmedNameOrNull();
        if (newName != null) {
            preset.setName(newName);
        }
        if (body.hasCategoryPatch()) {
            preset.setCategory(resolveCategory(body.getCategoryId()));
        }
        if (body.hasSubmittedUserPromptPatch()) {
            preset.setSubmittedUserPrompt(body.normalizedSubmittedUserPromptOrNull());
        }
        preset = presetRepository.save(preset);
        return toDto(preset, true, null);
    }

    private StylePresetDto persistPresetPreviewUpload(StylePresetEntity preset, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        String ct = file.getContentType() != null ? file.getContentType() : "";
        if (!ct.equals("image/png") && !ct.equals("image/webp") && !ct.equals("image/jpeg")) {
            throw new IllegalArgumentException("Supported types: image/png, image/webp, image/jpeg");
        }
        if (file.getSize() > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large (max 3MB)");
        }
        try {
            byte[] bytes = file.getBytes();
            if (preset.getPreviewImage() != null) {
                imageStorageService.deleteById(preset.getPreviewImage().getId());
                preset.setPreviewImage(null);
            }
            CachedImageEntity stored = imageStorageService.storeStylePresetPreview(preset.getId(), bytes, ct);
            preset.setPreviewImage(stored);
            preset = presetRepository.save(preset);
            return toDto(preset, true, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to store preview: " + e.getMessage());
        }
    }

    @Transactional
    public StylePresetDto uploadReferenceForGlobal(Long presetId, MultipartFile file) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (!Boolean.TRUE.equals(preset.getIsGlobal())) {
            throw new IllegalArgumentException("Only global presets use admin reference upload in this version");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        String ct = file.getContentType() != null ? file.getContentType() : "";
        if (!ct.equals("image/png") && !ct.equals("image/webp") && !ct.equals("image/jpeg")) {
            throw new IllegalArgumentException("Supported types: image/png, image/webp, image/jpeg");
        }
        if (file.getSize() > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large (max 3MB)");
        }
        try {
            byte[] bytes = file.getBytes();
            if (preset.getReferenceImage() != null) {
                imageStorageService.deleteById(preset.getReferenceImage().getId());
                preset.setReferenceImage(null);
            }
            CachedImageEntity stored = imageStorageService.storeStylePresetReference(presetId, bytes, ct);
            preset.setReferenceImage(stored);
            preset = presetRepository.save(preset);
            return toDto(preset, true, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to store preset reference: " + e.getMessage());
        }
    }

    @Transactional
    public StylePresetDto clearReferenceForGlobal(Long presetId) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (!Boolean.TRUE.equals(preset.getIsGlobal())) {
            throw new IllegalArgumentException("Only global presets use admin reference in this version");
        }
        if (preset.getReferenceImage() != null) {
            imageStorageService.deleteById(preset.getReferenceImage().getId());
            preset.setReferenceImage(null);
            preset = presetRepository.save(preset);
        }
        return toDto(preset, true, null);
    }

    /**
     * Получить пресет по ID.
     * userId может быть null (для admin-вызовов).
     */
    @Transactional(readOnly = true)
    public StylePresetDto getPresetById(Long presetId, Long userId) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        return toDto(preset, true, userId);
    }

    /**
     * Загрузить/заменить reference-изображение для пользовательского пресета.
     * Доступно только владельцу (preset.ownerId == userId).
     * Аналог uploadReferenceForGlobal для глобальных пресетов.
     */
    @Transactional
    public StylePresetDto uploadReferenceForOwner(Long presetId, Long userId,
                                                   org.springframework.web.multipart.MultipartFile file) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));

        if (preset.getOwner() == null || !userId.equals(preset.getOwner().getUserId())) {
            throw new IllegalArgumentException("Пресет не принадлежит пользователю");
        }
        return persistPresetReferenceUpload(preset, file, userId);
    }

    /**
     * Admin: заменить референсное изображение у любого неглобального пользовательского пресета
     * (модерация / правка автора). Глобальные пресеты — по-прежнему через
     * {@link #uploadReferenceForGlobal(long, MultipartFile)}.
     */
    @Transactional
    public StylePresetDto uploadReferenceForUserPresetAsAdmin(Long presetId, MultipartFile file) {
        StylePresetEntity preset = presetRepository.findByIdWithCategoryAndPreview(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));
        if (Boolean.TRUE.equals(preset.getIsGlobal())) {
            throw new IllegalArgumentException("Для глобального пресета используйте POST /api/generation/style-presets/{id}/reference");
        }
        if (preset.getOwner() == null) {
            throw new IllegalArgumentException("Пресет без владель не поддерживает пользовательский референс");
        }
        return persistPresetReferenceUpload(preset, file, null);
    }

    private StylePresetDto persistPresetReferenceUpload(StylePresetEntity preset, MultipartFile file,
                                                        Long viewerUserIdForPrivacy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
        String ct = file.getContentType() != null ? file.getContentType() : "";
        if (!ct.equals("image/png") && !ct.equals("image/webp") && !ct.equals("image/jpeg")) {
            throw new IllegalArgumentException("Поддерживаемые форматы: image/png, image/webp, image/jpeg");
        }
        if (file.getSize() > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("Файл слишком большой (максимум 3MB)");
        }
        try {
            byte[] bytes = file.getBytes();
            if (preset.getReferenceImage() != null) {
                imageStorageService.deleteById(preset.getReferenceImage().getId());
                preset.setReferenceImage(null);
            }
            var stored = imageStorageService.storeStylePresetReference(preset.getId(), bytes, ct);
            preset.setReferenceImage(stored);
            preset = presetRepository.save(preset);
            return toDto(preset, true, viewerUserIdForPrivacy);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Не удалось сохранить reference-изображение: " + e.getMessage());
        }
    }

    public StylePresetDto toDto(StylePresetEntity entity, boolean includeUi) {
        return toDto(entity, includeUi, null);
    }

    /**
     * @param viewerUserIdForPrivacy идентификатор пользователя, для которого строится ответ (миниапп);
     *                             {@code null} — без маскирования (админка, внутренние вызовы).
     */
    public StylePresetDto toDto(StylePresetEntity entity, boolean includeUi, Long viewerUserIdForPrivacy) {
        if (entity == null) {
            return null;
        }
        StylePresetDto d = new StylePresetDto();
        d.setId(entity.getId());
        d.setCode(entity.getCode());
        d.setName(entity.getName());
        d.setDescription(entity.getDescription());
        d.setSubmittedUserPrompt(entity.getSubmittedUserPrompt());
        d.setPromptSuffix(entity.getPromptSuffix());
        d.setRemoveBackground(entity.getRemoveBackground());
        d.setIsGlobal(entity.getIsGlobal());
        d.setOwnerId(entity.getOwner() != null ? entity.getOwner().getUserId() : null);
        d.setIsEnabled(entity.getIsEnabled());
        d.setSortOrder(entity.getSortOrder());
        d.setCategory(categoryToDto(entity.getCategory()));
        d.setCreatedAt(entity.getCreatedAt());
        d.setUpdatedAt(entity.getUpdatedAt());
        d.setModerationStatus(entity.getModerationStatus() != null
                ? entity.getModerationStatus().name() : null);
        d.setPublishedToCatalog(Boolean.TRUE.equals(entity.getPublishedToCatalog()));
        d.setUiMode(entity.getUiMode() != null ? entity.getUiMode().name() : StylePresetUiMode.STYLE_WITH_PROMPT.name());
        d.setRemoveBackgroundMode(
                entity.getRemoveBackgroundMode() != null
                        ? entity.getRemoveBackgroundMode().name()
                        : StylePresetRemoveBackgroundMode.PRESET_DEFAULT.name()
        );
        if (includeUi) {
            StylePresetPromptInputDto promptInput = presetPromptComposer.parsePromptInput(entity);
            if (viewerUserIdForPrivacy != null
                    && ConsumerStylePresetPolicy.hideFreestylePromptForConsumerMiniapp(entity, viewerUserIdForPrivacy)) {
                d.setHideFreestylePromptAuthorSupplied(true);
                promptInput = muteFreestylePromptSlotForConsumer(promptInput);
            }
            d.setPromptInput(promptInput);
            d.setFields(buildFieldsForDto(entity));
            if (entity.getPreviewImage() != null) {
                String url = imageStorageService.getPublicUrl(entity.getPreviewImage());
                d.setPreviewUrl(url);
                d.setPreviewMimeType(entity.getPreviewImage().getContentType());
                if (entity.getPreviewImage().getContentType() != null
                        && entity.getPreviewImage().getContentType().toLowerCase().contains("webp")) {
                    d.setPreviewWebpUrl(url);
                } else {
                    d.setPreviewWebpUrl(null);
                }
            }
            if (entity.getReferenceImage() != null) {
                String refUrl = imageStorageService.getPublicUrl(entity.getReferenceImage());
                d.setPresetReferenceImageUrl(refUrl);
                d.setPresetReferenceMimeType(entity.getReferenceImage().getContentType());
                d.setPresetReferenceSourceImageId(
                        StylePresetReferenceImageId.fromCachedImageId(entity.getReferenceImage().getId()));
            }
        } else if (viewerUserIdForPrivacy != null
                && ConsumerStylePresetPolicy.hideFreestylePromptForConsumerMiniapp(entity, viewerUserIdForPrivacy)) {
            d.setHideFreestylePromptAuthorSupplied(true);
        }
        if (viewerUserIdForPrivacy != null
                && ConsumerStylePresetPolicy.shouldMaskAuthorSecretsInApi(entity, viewerUserIdForPrivacy)) {
            d.setSubmittedUserPrompt(null);
        }
        if (viewerUserIdForPrivacy != null
                && ConsumerStylePresetPolicy.locksRemoveBackgroundUi(entity, viewerUserIdForPrivacy)) {
            d.setRemoveBackgroundLockedToPreset(true);
            d.setRemoveBackgroundEffective(ConsumerStylePresetPolicy.effectiveLockedRemoveBackgroundFromPreset(entity));
        }
        boolean shareable = StylePresetPublicSharePolicy.isShareableForPublicDeepLink(entity);
        d.setShareableAsDeepLink(shareable);
        if (shareable && entity.getId() != null) {
            String startParam = StylePresetDeepLinkParams.formatPresetId(entity.getId());
            d.setDeepLinkStartParam(startParam);
            String botUsername = appConfig.getTelegram() != null
                    ? appConfig.getTelegram().getBotUsername() : null;
            d.setDeepLinkUrl(StylePresetDeepLinkParams.telegramMiniAppShareUrl(botUsername, startParam));
        }
        return d;
    }

    /**
     * Сохраняет настройки вложений (referenceImages), отключает только свободный текст ({@code enabled=false}).
     */
    private static StylePresetPromptInputDto muteFreestylePromptSlotForConsumer(StylePresetPromptInputDto src) {
        StylePresetPromptInputDto out = new StylePresetPromptInputDto();
        if (src != null) {
            out.setPlaceholder(src.getPlaceholder());
            out.setMaxLength(src.getMaxLength());
            out.setReferenceImages(src.getReferenceImages());
        }
        out.setEnabled(Boolean.FALSE);
        out.setRequired(Boolean.FALSE);
        return out;
    }

    private StylePresetCategoryDto categoryToDto(StylePresetCategoryEntity category) {
        if (category == null) {
            return null;
        }
        StylePresetCategoryDto dto = new StylePresetCategoryDto();
        dto.setId(category.getId());
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        dto.setSortOrder(category.getSortOrder());
        return dto;
    }

    private StylePresetCategoryEntity resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return categoryRepository.findByCode(GENERAL_CATEGORY_CODE)
                    .orElseThrow(() -> new IllegalStateException("Default category '" + GENERAL_CATEGORY_CODE
                            + "' not found in database"));
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
    }

    private List<StylePresetFieldDto> parseFieldDtos(StylePresetEntity entity) {
        if (entity.getStructuredFieldsJson() == null) {
            return null;
        }
        return entity.getStructuredFieldsJson().stream()
                .map(m -> objectMapper.convertValue(m, StylePresetFieldDto.class))
                .toList();
    }

    /**
     * Поля из БД + виртуальное поле {@code preset_ref} (№1), если в шаблоне есть {@code {{preset_ref}}}
     * или уже сохранён файл референса — чтобы автор «своего стиля» сразу видел слот загрузки в miniapp.
     */
    private List<StylePresetFieldDto> buildFieldsForDto(StylePresetEntity entity) {
        List<StylePresetFieldDto> fromDb = parseFieldDtos(entity);
        if (!StylePresetPromptComposer.shouldExposePresetReferenceField(entity)) {
            return fromDb;
        }
        List<StylePresetFieldDto> out = new ArrayList<>();
        out.add(StylePresetSystemFields.presetReferenceFieldDefinition());
        if (fromDb != null) {
            for (StylePresetFieldDto f : fromDb) {
                if (f.getKey() != null && StylePresetSystemFields.isReservedFieldKey(f.getKey())) {
                    continue;
                }
                out.add(f);
            }
        }
        return out;
    }

    private void applyUiFields(StylePresetEntity preset, CreateStylePresetRequest request) {
        validateUiContract(copyRequestKeepingOnlyStructuralFields(request));
        preset.setRemoveBackground(request.getRemoveBackground());
        StylePresetRemoveBackgroundMode mode = resolveRemoveBackgroundMode(
                request.getRemoveBackgroundMode(),
                request.getRemoveBackground()
        );
        preset.setRemoveBackgroundMode(mode);
        syncLegacyRemoveBackground(preset, mode);
        if (request.getUiMode() != null && !request.getUiMode().isBlank()) {
            preset.setUiMode(StylePresetUiMode.valueOf(request.getUiMode().trim().toUpperCase()));
        } else {
            preset.setUiMode(StylePresetUiMode.STYLE_WITH_PROMPT);
        }
        if (request.getPromptInput() != null) {
            preset.setPromptInputJson(
                    objectMapper.convertValue(request.getPromptInput(), new TypeReference<Map<String, Object>>() { }));
        } else {
            preset.setPromptInputJson(null);
        }
        if (request.getFields() != null && !request.getFields().isEmpty()) {
            List<Map<String, Object>> list = request.getFields().stream()
                    .filter(f -> f.getKey() != null && !StylePresetSystemFields.isReservedFieldKey(f.getKey()))
                    .map(f -> objectMapper.convertValue(f, new TypeReference<Map<String, Object>>() { }))
                    .toList();
            preset.setStructuredFieldsJson(list.isEmpty() ? null : list);
        } else {
            preset.setStructuredFieldsJson(null);
        }
    }

    /**
     * Проверка согласованности suffix/полей без сохранения (шаблоны создания пользовательских пресетов).
     */
    public void validatePresetUiContract(CreateStylePresetRequest request) {
        validateUiContract(request);
    }

    /**
     * Контракт для JSON шаблона «создать свой пресет»: в {@code fields} может быть одна строка {@code preset_ref}
     * (подписи/обязательность для miniapp); в persisted {@code style_presets} она по-прежнему не хранится.
     */
    public void validateBlueprintPresetUiContract(CreateStylePresetRequest request) {
        List<StylePresetFieldDto> raw = request.getFields() == null ? List.of() : request.getFields();
        List<StylePresetFieldDto> presetRefUiRows = raw.stream()
                .filter(f -> f.getKey() != null
                        && StylePresetSystemFields.PRESET_REFERENCE_KEY.equals(f.getKey().trim()))
                .toList();
        if (presetRefUiRows.size() > 1) {
            throw new IllegalArgumentException("At most one preset_ref row allowed in blueprint fields");
        }
        if (presetRefUiRows.size() == 1) {
            validateBlueprintPresetRefUiRow(presetRefUiRows.get(0));
        }
        for (StylePresetFieldDto field : raw) {
            if (field.getKey() == null || field.getKey().isBlank()) {
                throw new IllegalArgumentException("Style preset field key is required");
            }
            String key = field.getKey().trim();
            if (StylePresetSystemFields.isReservedFieldKey(key)
                    && !StylePresetSystemFields.PRESET_REFERENCE_KEY.equals(key)) {
                throw new IllegalArgumentException("Reserved field key, use preset reference upload: " + key);
            }
        }
        validateUiContractCore(request, structuralFieldsOnly(raw), raw);
    }

    private void validateBlueprintPresetRefUiRow(StylePresetFieldDto field) {
        if (field.getType() == null || !"reference".equalsIgnoreCase(field.getType().trim())) {
            throw new IllegalArgumentException("preset_ref row in blueprint fields must have type \"reference\"");
        }
        int maxImg = field.getMaxImages() != null ? field.getMaxImages() : 1;
        int minImg = field.getMinImages() != null ? field.getMinImages() : 0;
        if (maxImg != 1) {
            throw new IllegalArgumentException("preset_ref in blueprint fields must have maxImages == 1");
        }
        if (minImg < 0 || minImg > 1) {
            throw new IllegalArgumentException("preset_ref in blueprint fields: minImages must be 0 or 1");
        }
        if (minImg > maxImg) {
            throw new IllegalArgumentException("minImages cannot exceed maxImages for preset_ref");
        }
    }

    private static boolean containsPresetRefUiRow(List<StylePresetFieldDto> fields) {
        if (fields == null) {
            return false;
        }
        return fields.stream().anyMatch(f -> f.getKey() != null
                && StylePresetSystemFields.PRESET_REFERENCE_KEY.equals(f.getKey().trim()));
    }

    private List<StylePresetFieldDto> structuralFieldsOnly(List<StylePresetFieldDto> fields) {
        if (fields == null || fields.isEmpty()) {
            return fields == null ? List.of() : fields;
        }
        return fields.stream()
                .filter(f -> f.getKey() == null || !StylePresetSystemFields.isReservedFieldKey(f.getKey().trim()))
                .toList();
    }

    /** Копия запроса без строк preset_ref в fields — для той же проверки, что при сохранении пресета в БД. */
    private CreateStylePresetRequest copyRequestKeepingOnlyStructuralFields(CreateStylePresetRequest request) {
        CreateStylePresetRequest copy = new CreateStylePresetRequest();
        copy.setPromptSuffix(request.getPromptSuffix());
        copy.setPromptInput(request.getPromptInput());
        copy.setRemoveBackground(request.getRemoveBackground());
        copy.setRemoveBackgroundMode(request.getRemoveBackgroundMode());
        copy.setUiMode(request.getUiMode());
        List<StylePresetFieldDto> structural = structuralFieldsOnly(request.getFields());
        copy.setFields(structural.isEmpty() ? null : structural);
        return copy;
    }

    private void validateUiContract(CreateStylePresetRequest request) {
        List<StylePresetFieldDto> fields = request.getFields() == null ? List.of() : request.getFields();
        for (StylePresetFieldDto field : fields) {
            if (field.getKey() == null || field.getKey().isBlank()) {
                throw new IllegalArgumentException("Style preset field key is required");
            }
            String key = field.getKey().trim();
            if (StylePresetSystemFields.isReservedFieldKey(key)) {
                throw new IllegalArgumentException("Reserved field key, use preset reference upload: " + key);
            }
        }
        validateUiContractCore(request, fields, fields);
    }

    private void validateUiContractCore(
            CreateStylePresetRequest request,
            List<StylePresetFieldDto> structuralFields,
            List<StylePresetFieldDto> fieldsForReferenceSum) {
        Set<String> keys = new HashSet<>();
        for (StylePresetFieldDto field : structuralFields) {
            String key = field.getKey().trim();
            if (!keys.add(key)) {
                throw new IllegalArgumentException("Duplicate style preset field key: " + key);
            }
        }

        Set<String> placeholders = StylePresetPromptComposer.extractPlaceholders(request.getPromptSuffix());
        for (String placeholder : placeholders) {
            if ("prompt".equals(placeholder)) {
                if (request.getPromptInput() != null && Boolean.FALSE.equals(request.getPromptInput().getEnabled())) {
                    throw new IllegalArgumentException("Template uses {{prompt}}, but prompt input is disabled");
                }
                continue;
            }
            if (StylePresetSystemFields.PRESET_REFERENCE_KEY.equals(placeholder)) {
                continue;
            }
            if (!keys.contains(placeholder)) {
                throw new IllegalArgumentException("Template placeholder has no matching field: " + placeholder);
            }
        }

        validateReferenceFields(request, fieldsForReferenceSum);
    }

    private void validateReferenceFields(CreateStylePresetRequest request, List<StylePresetFieldDto> fields) {
        StylePresetPromptInputDto promptInput = request.getPromptInput();
        int presetMaxRef = GenerationV2Constants.MAX_SOURCE_IMAGE_IDS;
        if (promptInput != null && promptInput.getReferenceImages() != null) {
            StylePresetReferenceInputDto ref = promptInput.getReferenceImages();
            if (ref.getMaxCount() != null) {
                if (ref.getMaxCount() > GenerationV2Constants.MAX_SOURCE_IMAGE_IDS) {
                    throw new IllegalArgumentException("referenceImages.maxCount cannot exceed "
                            + GenerationV2Constants.MAX_SOURCE_IMAGE_IDS);
                }
                presetMaxRef = ref.getMaxCount();
            }
            if (ref.getMinCount() != null && ref.getMinCount() < 0) {
                throw new IllegalArgumentException("referenceImages.minCount cannot be negative");
            }
        }

        int sumMax = 0;
        for (StylePresetFieldDto field : fields) {
            if (field.getType() == null || !"reference".equalsIgnoreCase(field.getType().trim())) {
                continue;
            }
            int minImg = field.getMinImages() != null ? field.getMinImages() : 0;
            int maxImg = field.getMaxImages() != null ? field.getMaxImages() : 1;
            if (minImg < 0 || maxImg < 1) {
                throw new IllegalArgumentException("Invalid minImages/maxImages for reference field: " + field.getKey());
            }
            if (minImg > maxImg) {
                throw new IllegalArgumentException("minImages cannot exceed maxImages for field: " + field.getKey());
            }
            sumMax += maxImg;
        }
        if (sumMax > presetMaxRef) {
            throw new IllegalArgumentException("Sum of reference slot maxImages (" + sumMax
                    + ") exceeds referenceImages.maxCount (" + presetMaxRef + ")");
        }
    }

    private void syncLegacyRemoveBackground(StylePresetEntity entity, StylePresetRemoveBackgroundMode mode) {
        switch (mode) {
            case FORCE_ON -> entity.setRemoveBackground(true);
            case FORCE_OFF -> entity.setRemoveBackground(false);
            case PRESET_DEFAULT -> entity.setRemoveBackground(null);
        }
    }

    private static StylePresetRemoveBackgroundMode resolveRemoveBackgroundMode(
            String modeStr,
            Boolean legacy) {
        if (modeStr != null && !modeStr.isBlank()) {
            return StylePresetRemoveBackgroundMode.valueOf(modeStr.trim().toUpperCase());
        }
        if (legacy == null) {
            return StylePresetRemoveBackgroundMode.PRESET_DEFAULT;
        }
        return legacy ? StylePresetRemoveBackgroundMode.FORCE_ON : StylePresetRemoveBackgroundMode.FORCE_OFF;
    }
}
