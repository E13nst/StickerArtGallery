package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StylePresetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetService.class);

    private final StylePresetRepository presetRepository;
    private final UserProfileService userProfileService;

    @Autowired
    public StylePresetService(
            StylePresetRepository presetRepository,
            UserProfileService userProfileService) {
        this.presetRepository = presetRepository;
        this.userProfileService = userProfileService;
    }

    /**
     * Получает все доступные пресеты для пользователя (глобальные + персональные)
     */
    @Transactional(readOnly = true)
    public List<StylePresetDto> getAvailablePresets(Long userId) {
        LOGGER.info("Getting available presets for user {}", userId);
        List<StylePresetEntity> presets = presetRepository.findAvailableForUser(userId);
        return presets.stream()
                .map(StylePresetDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получает все глобальные пресеты (для админа)
     */
    @Transactional(readOnly = true)
    public List<StylePresetDto> getAllGlobalPresets() {
        LOGGER.info("Getting all global presets");
        List<StylePresetEntity> presets = presetRepository.findAllGlobal();
        return presets.stream()
                .map(StylePresetDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получает персональные пресеты пользователя
     */
    @Transactional(readOnly = true)
    public List<StylePresetDto> getUserPresets(Long userId) {
        LOGGER.info("Getting user presets for user {}", userId);
        List<StylePresetEntity> presets = presetRepository.findByOwnerUserId(userId);
        return presets.stream()
                .map(StylePresetDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Создает глобальный пресет (только для админа)
     */
    @Transactional
    public StylePresetDto createGlobalPreset(CreateStylePresetRequest request) {
        LOGGER.info("Creating global preset: code={}", request.getCode());

        // Проверяем уникальность кода для глобальных пресетов
        presetRepository.findByCodeAndIsGlobalTrue(request.getCode())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Global preset with code '" + request.getCode() + "' already exists");
                });

        StylePresetEntity preset = new StylePresetEntity();
        preset.setCode(request.getCode());
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setPromptSuffix(request.getPromptSuffix());
        preset.setIsGlobal(true);
        preset.setOwner(null);
        preset.setIsEnabled(true);
        preset.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        preset = presetRepository.save(preset);
        LOGGER.info("Created global preset: id={}, code={}", preset.getId(), preset.getCode());
        return StylePresetDto.fromEntity(preset);
    }

    /**
     * Создает персональный пресет для пользователя
     */
    @Transactional
    public StylePresetDto createUserPreset(Long userId, CreateStylePresetRequest request) {
        LOGGER.info("Creating user preset for user {}: code={}", userId, request.getCode());

        UserProfileEntity profile = userProfileService.getOrCreateDefaultForUpdate(userId);

        // Проверяем уникальность кода для персональных пресетов пользователя
        presetRepository.findByCodeAndOwner_UserId(request.getCode(), userId)
                .ifPresent(p -> {
                    throw new IllegalArgumentException("User preset with code '" + request.getCode() + "' already exists");
                });

        StylePresetEntity preset = new StylePresetEntity();
        preset.setCode(request.getCode());
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setPromptSuffix(request.getPromptSuffix());
        preset.setIsGlobal(false);
        preset.setOwner(profile);
        preset.setIsEnabled(true);
        preset.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        preset = presetRepository.save(preset);
        LOGGER.info("Created user preset: id={}, code={}, userId={}", preset.getId(), preset.getCode(), userId);
        return StylePresetDto.fromEntity(preset);
    }

    /**
     * Обновляет пресет (только если пользователь имеет доступ)
     */
    @Transactional
    public StylePresetDto updatePreset(Long presetId, Long userId, CreateStylePresetRequest request, boolean isAdmin) {
        LOGGER.info("Updating preset: id={}, userId={}, isAdmin={}", presetId, userId, isAdmin);

        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));

        // Проверяем доступ
        if (!isAdmin && (!preset.getIsGlobal() && (preset.getOwner() == null || !preset.getOwner().getUserId().equals(userId)))) {
            throw new IllegalArgumentException("Access denied: preset is not accessible for user");
        }

        // Обновляем поля (код не меняем)
        preset.setName(request.getName());
        preset.setDescription(request.getDescription());
        preset.setPromptSuffix(request.getPromptSuffix());
        if (request.getSortOrder() != null) {
            preset.setSortOrder(request.getSortOrder());
        }

        preset = presetRepository.save(preset);
        LOGGER.info("Updated preset: id={}", presetId);
        return StylePresetDto.fromEntity(preset);
    }

    /**
     * Удаляет пресет (только если пользователь имеет доступ)
     */
    @Transactional
    public void deletePreset(Long presetId, Long userId, boolean isAdmin) {
        LOGGER.info("Deleting preset: id={}, userId={}, isAdmin={}", presetId, userId, isAdmin);

        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));

        // Проверяем доступ
        if (!isAdmin && (!preset.getIsGlobal() && (preset.getOwner() == null || !preset.getOwner().getUserId().equals(userId)))) {
            throw new IllegalArgumentException("Access denied: preset is not accessible for user");
        }

        presetRepository.delete(preset);
        LOGGER.info("Deleted preset: id={}", presetId);
    }

    /**
     * Включает/выключает пресет (только для админа)
     */
    @Transactional
    public StylePresetDto togglePresetEnabled(Long presetId, boolean enabled) {
        LOGGER.info("Toggling preset enabled: id={}, enabled={}", presetId, enabled);

        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetId));

        preset.setIsEnabled(enabled);
        preset = presetRepository.save(preset);
        LOGGER.info("Toggled preset enabled: id={}, enabled={}", presetId, enabled);
        return StylePresetDto.fromEntity(preset);
    }
}
