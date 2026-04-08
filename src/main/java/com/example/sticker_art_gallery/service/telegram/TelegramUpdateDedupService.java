package com.example.sticker_art_gallery.service.telegram;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простая in-memory дедупликация входящих Telegram updates.
 * Telegram может ретраить webhook-запросы, поэтому один и тот же update_id
 * иногда приходит повторно. Этот сервис защищает обработчики от дублей.
 */
@Service
public class TelegramUpdateDedupService {

    private static final long TTL_MILLIS = 10 * 60 * 1000L; // 10 минут достаточно для ретраев Telegram
    private static final int MAX_KEYS = 50_000;

    private final ConcurrentHashMap<Long, Long> seen = new ConcurrentHashMap<>();

    public boolean isDuplicate(long updateId) {
        if (updateId <= 0) {
            return false;
        }
        long now = Instant.now().toEpochMilli();
        cleanup(now);
        Long existing = seen.putIfAbsent(updateId, now);
        return existing != null;
    }

    private void cleanup(long now) {
        if (seen.size() > MAX_KEYS) {
            Iterator<Map.Entry<Long, Long>> it = seen.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Long> entry = it.next();
                if (now - entry.getValue() > TTL_MILLIS) {
                    it.remove();
                }
            }
            return;
        }
        // Легкая периодическая очистка при обычном размере.
        if ((now / 1000) % 30 == 0) {
            Iterator<Map.Entry<Long, Long>> it = seen.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Long> entry = it.next();
                if (now - entry.getValue() > TTL_MILLIS) {
                    it.remove();
                }
            }
        }
    }
}
