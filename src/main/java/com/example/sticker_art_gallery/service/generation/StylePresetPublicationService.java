package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.PresetPublicationRequestEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.generation.PresetPublicationRequestRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
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
    private static final long PUBLICATION_COST_ART = 10L;

    private final StylePresetRepository presetRepository;
    private final PresetPublicationRequestRepository publicationRequestRepository;
    private final ArtRewardService artRewardService;
    private final StylePresetService stylePresetService;

    public StylePresetPublicationService(StylePresetRepository presetRepository,
                                         PresetPublicationRequestRepository publicationRequestRepository,
                                         ArtRewardService artRewardService,
                                         StylePresetService stylePresetService) {
        this.presetRepository = presetRepository;
        this.publicationRequestRepository = publicationRequestRepository;
        this.artRewardService = artRewardService;
        this.stylePresetService = stylePresetService;
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
    public StylePresetDto publishPreset(Long userId, Long presetId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey не может быть пустым");
        }

        // Идемпотентность: если запрос уже обработан — возвращаем текущий пресет
        var existing = publicationRequestRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent() && "CHARGED".equals(existing.get().getStatus())) {
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

        // Списываем ART в той же транзакции
        try {
            artRewardService.award(
                    userId,
                    RULE_PUBLISH_PRESET,
                    PUBLICATION_COST_ART,
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
        presetRepository.save(preset);
        LOGGER.info("Пресет {} переведён в статус {} (модерация)", presetId, newStatus);

        return stylePresetService.getPresetById(presetId, null);
    }
}
