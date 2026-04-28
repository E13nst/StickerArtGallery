package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.PresetPublicationRequestEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.generation.PresetPublicationRequestRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import com.example.sticker_art_gallery.service.meme.MemeCandidatePromotionService;
import com.example.sticker_art_gallery.service.meme.MemeCandidateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

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
    private final ArtRewardService artRewardService;
    private final ArtRuleService artRuleService;
    private final StylePresetService stylePresetService;
    private final MemeCandidatePromotionService memeCandidatePromotionService;
    private final MemeCandidateService memeCandidateService;

    public StylePresetPublicationService(StylePresetRepository presetRepository,
                                         PresetPublicationRequestRepository publicationRequestRepository,
                                         ArtRewardService artRewardService,
                                         ArtRuleService artRuleService,
                                         StylePresetService stylePresetService,
                                         MemeCandidatePromotionService memeCandidatePromotionService,
                                         MemeCandidateService memeCandidateService) {
        this.presetRepository = presetRepository;
        this.publicationRequestRepository = publicationRequestRepository;
        this.artRewardService = artRewardService;
        this.artRuleService = artRuleService;
        this.stylePresetService = stylePresetService;
        this.memeCandidatePromotionService = memeCandidatePromotionService;
        this.memeCandidateService = memeCandidateService;
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
            memeCandidatePromotionService.promoteOnApproval(presetId);
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
        int affected = memeCandidateService.hideByStylePresetId(presetId);
        LOGGER.info("Preset takedown: presetId={}, affectedCandidates={}", presetId, affected);
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
        int affected = memeCandidateService.republishByStylePresetId(presetId);
        LOGGER.info("Preset republish: presetId={}, affectedCandidates={}", presetId, affected);
        memeCandidatePromotionService.promoteOnApproval(presetId);
        return stylePresetService.getPresetById(presetId, null);
    }
}
