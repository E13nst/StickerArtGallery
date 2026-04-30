package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.UpsertUserPresetCreationBlueprintRequest;
import com.example.sticker_art_gallery.dto.generation.UserPresetCreationBlueprintDto;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.UserPresetCreationBlueprintEntity;
import com.example.sticker_art_gallery.repository.UserPresetCreationBlueprintRepository;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Шаблоны формы создания пользовательских пресетов (контент задаётся в админке, не хардкодится во фронте).
 */
@Service
public class UserPresetCreationBlueprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPresetCreationBlueprintService.class);

    public static final String RULE_PUBLISH_PRESET = "PUBLISH_PRESET";

    private final UserPresetCreationBlueprintRepository repository;
    private final StylePresetService stylePresetService;
    private final ObjectMapper objectMapper;
    private final ArtRuleService artRuleService;

    public UserPresetCreationBlueprintService(UserPresetCreationBlueprintRepository repository,
                                              StylePresetService stylePresetService,
                                              ObjectMapper objectMapper,
                                              ArtRuleService artRuleService) {
        this.repository = repository;
        this.stylePresetService = stylePresetService;
        this.objectMapper = objectMapper;
        this.artRuleService = artRuleService;
    }

    @Transactional(readOnly = true)
    public List<UserPresetCreationBlueprintDto> listEnabledForUser() {
        Long cost = estimatePublicationCostArtOptional();
        return repository.findByEnabledTrueOrderBySortOrderAscIdAsc().stream()
                .map(entity -> {
                    UserPresetCreationBlueprintDto dto = mapBase(entity);
                    dto.setEstimatedPublicationCostArt(cost);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPresetCreationBlueprintDto> listAllForAdmin() {
        Long cost = estimatePublicationCostArtOptional();
        return repository.findAll().stream()
                .sorted((a, b) -> {
                    int s = Integer.compare(a.getSortOrder(), b.getSortOrder());
                    return s != 0 ? s : Long.compare(a.getId(), b.getId());
                })
                .map(entity -> {
                    UserPresetCreationBlueprintDto dto = mapBase(entity);
                    dto.setAdminTitle(entity.getAdminTitle());
                    dto.setEnabled(entity.getEnabled());
                    dto.setSortOrder(entity.getSortOrder());
                    dto.setEstimatedPublicationCostArt(cost);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserPresetCreationBlueprintDto findByCodeForValidation(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return repository.findByCode(code.trim())
                .filter(entity -> Boolean.TRUE.equals(entity.getEnabled()))
                .map(this::mapBase)
                .orElse(null);
    }

    /**
     * In-memory пресет по активному шаблону: генерация v2 без записи в {@code style_presets}.
     * Клиент передаёт {@code user_style_blueprint_code} и {@code preset_fields} (включая {@code preset_ref} с img_*).
     */
    @Transactional(readOnly = true)
    public StylePresetEntity buildTransientStylePresetForGeneration(String blueprintCode) {
        if (blueprintCode == null || blueprintCode.isBlank()) {
            throw new IllegalArgumentException("Код шаблона не может быть пустым");
        }
        UserPresetCreationBlueprintEntity entity = repository.findByCode(blueprintCode.trim())
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Шаблон создания пресета не найден или отключён: " + blueprintCode));
        try {
            CreateStylePresetRequest req = objectMapper.convertValue(
                    copyForValidation(entity.getPresetDefaultsJson()), CreateStylePresetRequest.class);
            req.setCode("_transient_blueprint_generation_");
            req.setName("_transient_blueprint_generation_");
            stylePresetService.validatePresetUiContract(req);
            return stylePresetService.materializeTransientPresetForGeneration(req);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Не удалось разобрать шаблон для генерации: " + e.getMessage(), e);
        }
    }

    @Transactional
    public UserPresetCreationBlueprintDto create(UpsertUserPresetCreationBlueprintRequest request) {
        String code = normalizeCode(request.getCode());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Шаблон с кодом уже существует: " + code);
        }
        validatePresetDefaultsPayload(request.getPresetDefaults());

        UserPresetCreationBlueprintEntity e = new UserPresetCreationBlueprintEntity();
        applyFields(e, request, code);

        UserPresetCreationBlueprintEntity saved = repository.save(e);
        LOGGER.info("Создан шаблон создания пресета: id={}, code={}", saved.getId(), saved.getCode());
        UserPresetCreationBlueprintDto dto = mapBase(saved);
        dto.setAdminTitle(saved.getAdminTitle());
        dto.setEnabled(saved.getEnabled());
        dto.setSortOrder(saved.getSortOrder());
        dto.setEstimatedPublicationCostArt(estimatePublicationCostArtOptional());
        return dto;
    }

    @Transactional
    public UserPresetCreationBlueprintDto update(Long id, UpsertUserPresetCreationBlueprintRequest request) {
        UserPresetCreationBlueprintEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Шаблон не найден: " + id));
        String code = normalizeCode(request.getCode());
        if (!code.equals(entity.getCode()) && repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("Шаблон с кодом уже существует: " + code);
        }
        validatePresetDefaultsPayload(request.getPresetDefaults());
        applyFields(entity, request, code);
        UserPresetCreationBlueprintEntity saved = repository.save(entity);
        LOGGER.info("Обновлён шаблон создания пресета id={}", saved.getId());
        UserPresetCreationBlueprintDto dto = mapBase(saved);
        dto.setAdminTitle(saved.getAdminTitle());
        dto.setEnabled(saved.getEnabled());
        dto.setSortOrder(saved.getSortOrder());
        dto.setEstimatedPublicationCostArt(estimatePublicationCostArtOptional());
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Шаблон не найден: " + id);
        }
        repository.deleteById(id);
        LOGGER.info("Удалён шаблон создания пресета id={}", id);
    }

    void validatePresetDefaultsPayload(Map<String, Object> presetDefaultsRaw) {
        if (presetDefaultsRaw == null || presetDefaultsRaw.isEmpty()) {
            throw new IllegalArgumentException("presetDefaults не может быть пустым");
        }
        try {
            CreateStylePresetRequest req = objectMapper.convertValue(copyForValidation(presetDefaultsRaw), CreateStylePresetRequest.class);
            req.setCode("_bp_code_");
            req.setName("_bp_name_");
            stylePresetService.validatePresetUiContract(req);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Не удалось разобрать presetDefaults как CreateStylePresetRequest: " + e.getMessage());
        }
    }

    /** Копируем дерево через JSON, чтобы нормализовать LinkedHashMap/списки. */
    private Map<String, Object> copyForValidation(Map<String, Object> raw) {
        if (raw == null) {
            return new LinkedHashMap<>();
        }
        return objectMapper.convertValue(raw, new TypeReference<Map<String, Object>>() { });
    }

    private void applyFields(UserPresetCreationBlueprintEntity entity, UpsertUserPresetCreationBlueprintRequest request,
                             String normalizedCode) {
        entity.setCode(normalizedCode);
        entity.setAdminTitle(request.getAdminTitle().trim());
        entity.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entity.setPresetDefaultsJson(copyForValidation(request.getPresetDefaults()));
        entity.setUiHintsJson(request.getUiHints() == null ? null : new LinkedHashMap<>(request.getUiHints()));
    }

    private UserPresetCreationBlueprintDto mapBase(UserPresetCreationBlueprintEntity e) {
        UserPresetCreationBlueprintDto dto = new UserPresetCreationBlueprintDto();
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setPresetDefaults(copyForValidation(e.getPresetDefaultsJson()));
        dto.setUiHints(e.getUiHintsJson() != null ? new LinkedHashMap<>(e.getUiHintsJson()) : null);
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code обязателен");
        }
        return code.trim();
    }

    /** Стоимость публикации каталогом (по активному правилу) или null. */
    private Long estimatePublicationCostArtOptional() {
        return artRuleService.findByCode(RULE_PUBLISH_PRESET)
                .filter(r -> Boolean.TRUE.equals(r.getIsEnabled()))
                .map(r -> Math.abs(r.getAmount()))
                .orElse(null);
    }
}
