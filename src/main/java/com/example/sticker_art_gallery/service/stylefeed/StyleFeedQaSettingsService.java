package com.example.sticker_art_gallery.service.stylefeed;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedQaRepeatRatedStateDto;
import com.example.sticker_art_gallery.model.admin.GalleryKvSettingEntity;
import com.example.sticker_art_gallery.repository.GalleryKvSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QA: для одного telegram user id лента возвращает уже оценённые карточки.
 * Значение задаётся в админке (БД); если записи нет — используется {@code app.style-feed} из конфигурации.
 */
@Service
public class StyleFeedQaSettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFeedQaSettingsService.class);

    public static final String QA_REPEAT_RATED_TELEGRAM_USER_KEY = "style_feed.qa_repeat_rated_telegram_user_id";

    private final GalleryKvSettingRepository galleryKvSettingRepository;
    private final AppConfig appConfig;

    public StyleFeedQaSettingsService(GalleryKvSettingRepository galleryKvSettingRepository,
                                      AppConfig appConfig) {
        this.galleryKvSettingRepository = galleryKvSettingRepository;
        this.appConfig = appConfig;
    }

    /**
     * Активен ли режим повторной выдачи для данного пользователя.
     */
    public boolean isRepeatRatedQaActiveForUser(Long telegramUserId) {
        long configured = getEffectiveRepeatRatedTelegramUserIdCached();
        return telegramUserId != null && configured > 0 && configured == telegramUserId;
    }

    @Cacheable(cacheNames = "galleryKvStyleFeedQa", key = "'effective'")
    @Transactional(readOnly = true)
    public long getEffectiveRepeatRatedTelegramUserIdCached() {
        return computeEffectiveRepeatRatedTelegramUserId();
    }

    private long computeEffectiveRepeatRatedTelegramUserId() {
        return galleryKvSettingRepository.findById(QA_REPEAT_RATED_TELEGRAM_USER_KEY)
                .map(e -> normalizeStoredUserId(e.getSettingValue()))
                .orElseGet(this::environmentFallbackUserId);
    }

    @Transactional(readOnly = true)
    public StyleFeedQaRepeatRatedStateDto getStateForAdmin() {
        StyleFeedQaRepeatRatedStateDto dto = new StyleFeedQaRepeatRatedStateDto();
        long env = environmentFallbackUserId();
        dto.setEnvironmentFallbackUserId(env);
        var row = galleryKvSettingRepository.findById(QA_REPEAT_RATED_TELEGRAM_USER_KEY);
        dto.setDatabaseOverridePresent(row.isPresent());
        dto.setDatabaseValueOrNull(row.map(GalleryKvSettingEntity::getSettingValue).orElse(null));
        dto.setEffectiveTelegramUserId(computeEffectiveRepeatRatedTelegramUserId());
        return dto;
    }

    /**
     * @param telegramUserId {@code null} — удалить запись из БД (действует только env).
     *                      {@code 0} — явно выключить QA (перекрывает env).
     *                      {@code >0} — включить для этого user id.
     */
    @Transactional
    @CacheEvict(cacheNames = "galleryKvStyleFeedQa", allEntries = true)
    public StyleFeedQaRepeatRatedStateDto saveRepeatRatedTelegramUserId(Long telegramUserId) {
        if (telegramUserId == null) {
            galleryKvSettingRepository.deleteById(QA_REPEAT_RATED_TELEGRAM_USER_KEY);
            LOGGER.info("QA style-feed repeat-rated: снято переопределение в БД, используется env");
        } else {
            if (telegramUserId < 0) {
                throw new IllegalArgumentException("telegramUserId не может быть отрицательным");
            }
            GalleryKvSettingEntity e = new GalleryKvSettingEntity();
            e.setSettingKey(QA_REPEAT_RATED_TELEGRAM_USER_KEY);
            e.setSettingValue(Long.toString(telegramUserId));
            galleryKvSettingRepository.save(e);
            LOGGER.info("QA style-feed repeat-rated: в БД записан userId={}", telegramUserId);
        }
        return getStateForAdmin();
    }

    private long normalizeStoredUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            return environmentFallbackUserId();
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            LOGGER.warn("Некорректное значение gallery_kv для {}: {}", QA_REPEAT_RATED_TELEGRAM_USER_KEY, raw);
            return environmentFallbackUserId();
        }
    }

    private long environmentFallbackUserId() {
        Long x = appConfig.getStyleFeed().getQaRepeatRatedTelegramUserId();
        return x != null && x > 0 ? x : 0L;
    }
}
