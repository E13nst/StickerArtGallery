package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetTelegramCacheEntity;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.repository.StickerSetTelegramCacheRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.Executor;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StickerSetTelegramCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetTelegramCacheService.class);

    private final StickerSetTelegramCacheRepository cacheRepository;
    private final StickerSetRepository stickerSetRepository;
    private final TelegramBotApiService telegramBotApiService;
    private final ObjectMapper objectMapper;
    private final Executor stickerCacheRefreshExecutor;
    private final Set<Long> refreshInFlight = ConcurrentHashMap.newKeySet();
    private final long refreshAfterDays;

    public StickerSetTelegramCacheService(
            StickerSetTelegramCacheRepository cacheRepository,
            StickerSetRepository stickerSetRepository,
            TelegramBotApiService telegramBotApiService,
            ObjectMapper objectMapper,
            @Qualifier("stickerCacheRefreshExecutor") Executor stickerCacheRefreshExecutor,
            @Value("${app.sticker-cache.refresh-after-days:7}") long refreshAfterDays) {
        this.cacheRepository = cacheRepository;
        this.stickerSetRepository = stickerSetRepository;
        this.telegramBotApiService = telegramBotApiService;
        this.objectMapper = objectMapper;
        this.stickerCacheRefreshExecutor = stickerCacheRefreshExecutor;
        this.refreshAfterDays = refreshAfterDays;
    }

    public Optional<Object> getCachedPayload(Long stickersetId) {
        return cacheRepository.findById(stickersetId)
                .flatMap(cache -> deserializePayload(stickersetId, cache.getTelegramPayload()));
    }

    public boolean isStale(Long stickersetId) {
        return cacheRepository.findById(stickersetId)
                .map(cache -> cache.getRefreshAfter().isBefore(OffsetDateTime.now()))
                .orElse(true);
    }

    public void scheduleRefreshIfNeeded(Long stickersetId) {
        if (stickersetId == null) {
            return;
        }

        boolean shouldRefresh = isStale(stickersetId);
        if (!shouldRefresh) {
            return;
        }

        if (!refreshInFlight.add(stickersetId)) {
            LOGGER.debug("⏳ Refresh already in progress for stickerset {}", stickersetId);
            return;
        }

        stickerCacheRefreshExecutor.execute(() -> refreshNow(stickersetId));
    }

    @Transactional
    public void save(Long stickersetId, String stickersetName, Object telegramPayload) {
        if (stickersetId == null || telegramPayload == null) {
            return;
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(telegramPayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize telegram payload", e);
        }

        Integer stickersCount = telegramBotApiService.extractStickersCountFromStickerSetInfo(telegramPayload);
        OffsetDateTime now = OffsetDateTime.now();

        StickerSetTelegramCacheEntity cache = cacheRepository.findById(stickersetId)
                .orElseGet(() -> {
                    StickerSetTelegramCacheEntity entity = new StickerSetTelegramCacheEntity();
                    entity.setStickersetId(stickersetId);
                    return entity;
                });

        cache.setTelegramPayload(payloadJson);
        cache.setStickersCount(stickersCount == null ? 0 : stickersCount);
        cache.setSyncedAt(now);
        cache.setRefreshAfter(now.plusDays(refreshAfterDays));
        cacheRepository.save(cache);

        stickerSetRepository.findById(stickersetId).ifPresent(stickerSet -> {
            boolean updated = false;
            String title = telegramBotApiService.extractTitleFromStickerSetInfo(telegramPayload);
            if (title != null && !title.equals(stickerSet.getTitle())) {
                stickerSet.setTitle(title);
                updated = true;
            }
            if (stickersCount != null && !stickersCount.equals(stickerSet.getStickersCount())) {
                stickerSet.setStickersCount(stickersCount);
                updated = true;
            }
            if (updated) {
                stickerSetRepository.save(stickerSet);
            }
        });

        LOGGER.debug("💾 Saved telegram cache for stickerset {} ({})", stickersetId, stickersetName);
    }

    public void refreshNow(Long stickersetId) {
        try {
            StickerSet stickerSet = stickerSetRepository.findById(stickersetId).orElse(null);
            if (stickerSet == null) {
                return;
            }

            Object payload = telegramBotApiService.getStickerSetInfo(stickerSet.getName());
            save(stickersetId, stickerSet.getName(), payload);
        } catch (Exception e) {
            LOGGER.warn("⚠️ Failed to refresh telegram cache for stickerset {}: {}", stickersetId, e.getMessage());
        } finally {
            refreshInFlight.remove(stickersetId);
        }
    }

    private Optional<Object> deserializePayload(Long stickersetId, String payload) {
        try {
            return Optional.ofNullable(objectMapper.readValue(payload, Object.class));
        } catch (Exception e) {
            LOGGER.warn("⚠️ Failed to deserialize cached telegram payload for stickerset {}: {}", stickersetId, e.getMessage());
            return Optional.empty();
        }
    }
}
