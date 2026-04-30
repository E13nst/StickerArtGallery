package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.PublishUserStyleFromTaskRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetSystemFields;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.PresetPublicationFromGenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.PresetPublicationRequestEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.generation.PresetPublicationFromGenerationTaskRepository;
import com.example.sticker_art_gallery.repository.generation.PresetPublicationRequestRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemPromotionService;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис публикации пользовательского пресета в публичный каталог.
 * <p>
 * Правила:
 * <ol>
 *   <li>Пресет должен принадлежать пользователю (ownerId == userId).</li>
 *   <li>Статус DRAFT → PENDING_MODERATION.</li>
 *   <li>Списывается 10 ART-points через ArtRewardService.</li>
 *   <li>idempotency_key гарантирует, что при повторном запросе ART не списывается дважды.</li>
 *   <li>Списание и смена статуса выполняются в одной транзакции.</li>
 * </ol>
 */
@Service
@Transactional
public class StylePresetPublicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StylePresetPublicationService.class);

    private static final String RULE_PUBLISH_PRESET = "PUBLISH_PRESET";

    private final StylePresetRepository presetRepository;
    private final PresetPublicationRequestRepository publicationRequestRepository;
    private final PresetPublicationFromGenerationTaskRepository publicationFromTaskRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final ArtRewardService artRewardService;
    private final ArtRuleService artRuleService;
    private final StylePresetService stylePresetService;
    private final UserPresetCreationBlueprintService userPresetCreationBlueprintService;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;
    private final StyleFeedItemPromotionService styleFeedItemPromotionService;
    private final StyleFeedItemService styleFeedItemService;

    public StylePresetPublicationService(StylePresetRepository presetRepository,
                                         PresetPublicationRequestRepository publicationRequestRepository,
                                         PresetPublicationFromGenerationTaskRepository publicationFromTaskRepository,
                                         GenerationTaskRepository generationTaskRepository,
                                         ArtRewardService artRewardService,
                                         ArtRuleService artRuleService,
                                         StylePresetService stylePresetService,
                                         UserPresetCreationBlueprintService userPresetCreationBlueprintService,
                                         ImageStorageService imageStorageService,
                                         ObjectMapper objectMapper,
                                         StyleFeedItemPromotionService styleFeedItemPromotionService,
                                         StyleFeedItemService styleFeedItemService) {
        this.presetRepository = presetRepository;
        this.publicationRequestRepository = publicationRequestRepository;
        this.publicationFromTaskRepository = publicationFromTaskRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.artRewardService = artRewardService;
        this.artRuleService = artRuleService;
        this.stylePresetService = stylePresetService;
        this.userPresetCreationBlueprintService = userPresetCreationBlueprintService;
        this.imageStorageService = imageStorageService;
        this.objectMapper = objectMapper;
        this.styleFeedItemPromotionService = styleFeedItemPromotionService;
        this.styleFeedItemService = styleFeedItemService;
    }

    /**
     * Опубликовать пресет (перевести в PENDING_MODERATION, списать 10 ART).
     * Идемпотентен по idempotencyKey.
     *
     * @param userId         Telegram ID пользователя (владелец пресета)
     * @param presetId       ID пресета
     * @param idempotencyKey Клиентский UUID запроса
     * @return DTO обновлённого пресета
     */
    public StylePresetDto publishPreset(Long userId, Long presetId, String idempotencyKey, String displayName,
                                        Boolean consentResultPublicShow) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey не может быть пустым");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName не может быть пустым");
        }
        String normalizedDisplayName = displayName.trim();
        if (normalizedDisplayName.length() > 100) {
            throw new IllegalArgumentException("displayName не может быть длиннее 100 символов");
        }
        if (!Boolean.TRUE.equals(consentResultPublicShow)) {
            throw new IllegalArgumentException("consentResultPublicShow должен быть true");
        }

        // Идемпотентность: если запрос уже обработан — возвращаем текущий пресет
        var existing = publicationRequestRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent() && "CHARGED".equals(existing.get().getStatus())) {
            if (!existing.get().getPreset().getId().equals(presetId)) {
                throw new IllegalArgumentException("idempotencyKey уже использован для другого пресета");
            }
            LOGGER.info("Дублированный запрос публикации: idempotencyKey={}, presetId={}", idempotencyKey, presetId);
            return stylePresetService.getPresetById(presetId, userId);
        }

        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Пресет не найден: " + presetId));

        // Проверка владельца
        if (preset.getOwner() == null || !userId.equals(preset.getOwner().getUserId())) {
            throw new IllegalArgumentException("Пресет не принадлежит пользователю");
        }

        // Проверка статуса
        if (preset.getModerationStatus() != PresetModerationStatus.DRAFT) {
            throw new IllegalStateException("Пресет не в статусе DRAFT (текущий: " + preset.getModerationStatus() + ")");
        }

        // Создаём запись idempotency (или получаем существующий PENDING)
        PresetPublicationRequestEntity request = existing.orElseGet(() -> {
            PresetPublicationRequestEntity r = new PresetPublicationRequestEntity();
            r.setPreset(preset);
            r.setIdempotencyKey(idempotencyKey);
            r.setStatus("PENDING");
            return publicationRequestRepository.save(r);
        });
        request.setDisplayName(normalizedDisplayName);
        request.setConsentAt(OffsetDateTime.now());
        publicationRequestRepository.save(request);

        preset.setName(normalizedDisplayName);
        preset.setPublicShowConsentAt(request.getConsentAt());
        preset.setPublishedToCatalog(false);
        presetRepository.save(preset);

        long publicationCost = artRuleService.getEnabledRuleOrThrow(RULE_PUBLISH_PRESET).getAmount();

        // Списываем ART в той же транзакции
        try {
            artRewardService.award(
                    userId,
                    RULE_PUBLISH_PRESET,
                    publicationCost,
                    "{\"presetId\":" + presetId + ",\"idempotencyKey\":\"" + idempotencyKey + "\"}",
                    "publish-preset:" + presetId + ":" + idempotencyKey,
                    userId
            );
        } catch (Exception e) {
            request.setStatus("FAILED");
            publicationRequestRepository.save(request);
            throw new IllegalStateException("Недостаточно ART для публикации пресета: " + e.getMessage(), e);
        }

        // Фиксируем списание и смену статуса
        request.setStatus("CHARGED");
        request.setChargedAt(OffsetDateTime.now());
        publicationRequestRepository.save(request);

        preset.setModerationStatus(PresetModerationStatus.PENDING_MODERATION);
        presetRepository.save(preset);

        LOGGER.info("Пресет {} отправлен на модерацию пользователем {}", presetId, userId);
        return stylePresetService.getPresetById(presetId, userId);
    }

    /**
     * Публикация стиля из завершённой задачи генерации v2 ({@code userStyleBlueprintCode}), без черновика пресета.
     */
    public StylePresetDto publishUserStyleFromCompletedGenerationTask(Long userId, String taskId,
                                                                      PublishUserStyleFromTaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request не может быть null");
        }
        String idempotencyKey = request.getIdempotencyKey();
        String displayName = request.getDisplayName();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey не может быть пустым");
        }
        if (!Boolean.TRUE.equals(request.getConsentResultPublicShow())) {
            throw new IllegalArgumentException("consentResultPublicShow должен быть true");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName не может быть пустым");
        }
        String normalizedDisplayName = displayName.trim();
        if (normalizedDisplayName.length() > 100) {
            throw new IllegalArgumentException("displayName не может быть длиннее 100 символов");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("code не может быть пустым");
        }

        Optional<PresetPublicationFromGenerationTaskEntity> idemExisting =
                publicationFromTaskRepository.findByIdempotencyKey(idempotencyKey);
        if (idemExisting.isPresent() && "CHARGED".equals(idemExisting.get().getStatus())) {
            PresetPublicationFromGenerationTaskEntity row = idemExisting.get();
            if (!row.getGenerationTaskId().equals(taskId)) {
                throw new IllegalArgumentException("idempotencyKey уже использован для другой задачи");
            }
            Long presetId = row.getPreset().getId();
            LOGGER.info("Дубль публикации из задачи: idempotencyKey={}, taskId={}, presetId={}",
                    idempotencyKey, taskId, presetId);
            return stylePresetService.getPresetById(presetId, userId);
        }
        idemExisting.filter(r -> "FAILED".equals(r.getStatus()))
                .ifPresent(publicationFromTaskRepository::delete);
        final Optional<PresetPublicationFromGenerationTaskEntity> reopenIdemAfterFailed =
                publicationFromTaskRepository.findByIdempotencyKey(idempotencyKey);

        GenerationTaskEntity task = generationTaskRepository.findByTaskIdForUpdate(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена: " + taskId));
        if (task.getUserProfile() == null || !userId.equals(task.getUserProfile().getUserId())) {
            throw new IllegalArgumentException("Задача не принадлежит пользователю");
        }
        if (task.getStatus() != GenerationTaskStatus.COMPLETED) {
            throw new IllegalStateException("Задача должна быть в статусе COMPLETED");
        }

        Map<String, Object> metadata = parseGenerationMetadata(task.getMetadata());
        if (!"generation-v2".equals(String.valueOf(metadata.getOrDefault("flow", "")).trim())) {
            throw new IllegalArgumentException("Неподдерживаемый тип задачи (ожидается generation-v2)");
        }

        Object stylePresetRaw = metadata.get("stylePresetId");
        if (stylePresetRaw != null) {
            if (stylePresetRaw instanceof Number num && num.longValue() != 0) {
                throw new IllegalArgumentException("Публикация из задачи доступна только без stylePresetId");
            }
            if (stylePresetRaw instanceof String s && !s.isBlank()) {
                try {
                    if (Long.parseLong(s.trim()) != 0) {
                        throw new IllegalArgumentException("Публикация из задачи доступна только без stylePresetId");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Публикация из задачи доступна только без stylePresetId");
                }
            }
        }

        String blueprintMeta = normalizeBlueprintCode(metadata.get("userStyleBlueprintCode"));
        if (blueprintMeta == null) {
            throw new IllegalArgumentException("Задача не из потока «свой стиль» (нет userStyleBlueprintCode)");
        }
        if (request.getUserStyleBlueprintCode() != null && !request.getUserStyleBlueprintCode().isBlank()) {
            String fromBody = request.getUserStyleBlueprintCode().trim();
            if (!fromBody.equalsIgnoreCase(blueprintMeta)) {
                throw new IllegalArgumentException("user_style_blueprint_code не совпадает с задачей");
            }
        }

        if (publicationFromTaskRepository.existsByGenerationTaskIdAndStatus(taskId, "CHARGED")) {
            throw new IllegalStateException("Задача уже опубликована");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> presetFields = metadata.get("preset_fields") instanceof Map
                ? (Map<String, Object>) metadata.get("preset_fields") : Collections.emptyMap();
        List<String> presetRefSlots = StylePresetPromptComposer.parseReferenceIds(
                presetFields.get(StylePresetSystemFields.PRESET_REFERENCE_KEY));
        if (presetRefSlots.isEmpty()) {
            throw new IllegalArgumentException("В задаче отсутствует preset_ref");
        }
        String firstRefImg = presetRefSlots.get(0);
        UUID refCachedId = StylePresetReferenceImageId.parseCachedImageId(firstRefImg).orElseThrow(
                () -> new IllegalArgumentException(
                        "Опорное фото должно быть сохранено в галерее как img_sagref_*"));

        ImageStorageService.CachedImageBlob refBlob = imageStorageService.readCachedImageBlob(refCachedId);

        CreateStylePresetRequest merged = userPresetCreationBlueprintService.mergeDefaultsWithPublicationOverlay(
                blueprintMeta,
                request.getCode().trim(),
                normalizedDisplayName,
                request.getDescription(),
                request.getCategoryId(),
                request.getSortOrder());

        StylePresetDto createdPreset = stylePresetService.createUserPreset(userId, merged);
        Long presetId = createdPreset.getId();
        if (presetId == null) {
            throw new IllegalStateException("Созданный пресет не имеет ID");
        }

        var storedReference = imageStorageService.storeStylePresetReference(
                presetId, refBlob.data(), refBlob.contentType());

        StylePresetEntity presetEntity = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalStateException("Пресет не найден после создания: " + presetId));
        presetEntity.setReferenceImage(storedReference);

        UUID resultImageId = task.getCachedImageId();
        if (resultImageId != null) {
            ImageStorageService.CachedImageBlob previewBlob = imageStorageService.readCachedImageBlob(resultImageId);
            var previewEntity = imageStorageService.storeStylePresetPreview(
                    presetId, previewBlob.data(), previewBlob.contentType());
            presetEntity.setPreviewImage(previewEntity);
        }

        presetEntity.setPublishedToCatalog(false);
        presetEntity.setModerationStatus(PresetModerationStatus.DRAFT);
        presetRepository.save(presetEntity);

        PresetPublicationFromGenerationTaskEntity pubRow = reopenIdemAfterFailed.orElseGet(() -> {
            PresetPublicationFromGenerationTaskEntity r = new PresetPublicationFromGenerationTaskEntity();
            r.setGenerationTaskId(taskId);
            r.setPreset(presetEntity);
            r.setIdempotencyKey(idempotencyKey);
            r.setStatus("PENDING");
            return publicationFromTaskRepository.save(r);
        });
        if (!presetId.equals(pubRow.getPreset().getId())) {
            throw new IllegalArgumentException("idempotencyKey уже привязан к другому пресету");
        }
        pubRow.setDisplayName(normalizedDisplayName);
        pubRow.setConsentAt(OffsetDateTime.now());
        publicationFromTaskRepository.save(pubRow);

        presetEntity.setPublicShowConsentAt(pubRow.getConsentAt());
        presetEntity.setName(normalizedDisplayName);
        presetRepository.save(presetEntity);

        long publicationCost = artRuleService.getEnabledRuleOrThrow(RULE_PUBLISH_PRESET).getAmount();
        String rewardMeta;
        try {
            rewardMeta = objectMapper.writeValueAsString(Map.of(
                    "presetId", presetId,
                    "taskId", taskId,
                    "idempotencyKey", idempotencyKey));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сформировать метадату списания", e);
        }
        String rewardRefKey = "publish-from-task:" + taskId + ":" + idempotencyKey;
        try {
            artRewardService.award(
                    userId,
                    RULE_PUBLISH_PRESET,
                    publicationCost,
                    rewardMeta,
                    rewardRefKey,
                    userId
            );
        } catch (Exception e) {
            pubRow.setStatus("FAILED");
            publicationFromTaskRepository.save(pubRow);
            throw new IllegalStateException("Недостаточно ART для публикации пресета: " + e.getMessage(), e);
        }

        pubRow.setStatus("CHARGED");
        pubRow.setChargedAt(OffsetDateTime.now());
        publicationFromTaskRepository.save(pubRow);

        presetEntity.setModerationStatus(PresetModerationStatus.PENDING_MODERATION);
        presetRepository.save(presetEntity);

        LOGGER.info("Пресет {} из задачи {} отправлен на модерацию пользователем {}",
                presetId, taskId, userId);
        return stylePresetService.getPresetById(presetId, userId);
    }

    /**
     * Автор снимает одобренный стиль с публичной витрины (аналог админского takedown без прав администратора).
     */
    public StylePresetDto ownerUnpublishPresetFromCatalog(Long userId, Long presetId) {
        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Пресет не найден: " + presetId));
        if (preset.getOwner() == null || !userId.equals(preset.getOwner().getUserId())) {
            throw new IllegalArgumentException("Пресет не принадлежит пользователю");
        }
        if (preset.getModerationStatus() != PresetModerationStatus.APPROVED) {
            throw new IllegalStateException("Снять с витрины можно только одобренный пресет");
        }
        if (!Boolean.TRUE.equals(preset.getPublishedToCatalog())) {
            return stylePresetService.getPresetById(presetId, userId);
        }
        preset.setPublishedToCatalog(false);
        presetRepository.save(preset);
        int affected = styleFeedItemService.hideByStylePresetId(presetId);
        LOGGER.info("Owner takedown: presetId={}, userId={}, affectedStyleFeedItems={}", presetId, userId, affected);
        return stylePresetService.getPresetById(presetId, userId);
    }

    private Map<String, Object> parseGenerationMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() { });
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            LOGGER.warn("Не удалось разобрать metadata задачи генерации");
            return Collections.emptyMap();
        }
    }

    private static String normalizeBlueprintCode(Object raw) {
        if (raw instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return null;
    }

    /**
     * Admin: изменить статус модерации пресета (PENDING_MODERATION → APPROVED | REJECTED).
     */
    public StylePresetDto moderatePreset(Long presetId, PresetModerationStatus newStatus) {
        if (newStatus != PresetModerationStatus.APPROVED && newStatus != PresetModerationStatus.REJECTED) {
            throw new IllegalArgumentException("Допустимые статусы модерации: APPROVED, REJECTED");
        }

        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Пресет не найден: " + presetId));

        if (preset.getModerationStatus() != PresetModerationStatus.PENDING_MODERATION) {
            throw new IllegalStateException("Пресет не в статусе PENDING_MODERATION");
        }

        preset.setModerationStatus(newStatus);
        if (newStatus == PresetModerationStatus.APPROVED) {
            preset.setPublishedToCatalog(true);
        } else {
            preset.setPublishedToCatalog(false);
        }
        presetRepository.save(preset);
        LOGGER.info("Пресет {} переведён в статус {} (модерация)", presetId, newStatus);
        if (newStatus == PresetModerationStatus.APPROVED) {
            styleFeedItemPromotionService.promoteOnApproval(presetId);
        }

        return stylePresetService.getPresetById(presetId, null);
    }

    public StylePresetDto takedownPreset(Long presetId) {
        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Пресет не найден: " + presetId));
        if (preset.getModerationStatus() != PresetModerationStatus.APPROVED) {
            throw new IllegalStateException("Снять с витрины можно только APPROVED пресет");
        }
        preset.setPublishedToCatalog(false);
        presetRepository.save(preset);
        int affected = styleFeedItemService.hideByStylePresetId(presetId);
        LOGGER.info("Preset takedown: presetId={}, affectedStyleFeedItems={}", presetId, affected);
        return stylePresetService.getPresetById(presetId, null);
    }

    public StylePresetDto republishPreset(Long presetId) {
        StylePresetEntity preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Пресет не найден: " + presetId));
        if (preset.getModerationStatus() != PresetModerationStatus.APPROVED) {
            throw new IllegalStateException("Вернуть на витрину можно только APPROVED пресет");
        }
        if (preset.getPublicShowConsentAt() == null) {
            throw new IllegalStateException("Отсутствует согласие автора на публичный показ");
        }
        preset.setPublishedToCatalog(true);
        presetRepository.save(preset);
        int affected = styleFeedItemService.republishByStylePresetId(presetId);
        LOGGER.info("Preset republish: presetId={}, affectedStyleFeedItems={}", presetId, affected);
        styleFeedItemPromotionService.promoteOnApproval(presetId);
        return stylePresetService.getPresetById(presetId, null);
    }
}
