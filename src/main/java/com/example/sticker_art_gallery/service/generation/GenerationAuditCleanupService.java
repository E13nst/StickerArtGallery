package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.GenerationAuditSessionEntity;
import com.example.sticker_art_gallery.repository.GenerationAuditSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Удаление audit-записей генерации старше 90 дней (по expires_at).
 * События удаляются каскадно при удалении сессий.
 */
@Service
public class GenerationAuditCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationAuditCleanupService.class);

    private final GenerationAuditSessionRepository sessionRepository;

    @Autowired
    public GenerationAuditCleanupService(GenerationAuditSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3:00 (после cleanup generation_tasks в 2:00)
    @Transactional
    public void cleanupExpiredAuditSessions() {
        OffsetDateTime now = OffsetDateTime.now();
        List<GenerationAuditSessionEntity> expired = sessionRepository.findByExpiresAtBefore(now);
        if (expired.isEmpty()) {
            LOGGER.debug("No expired generation audit sessions to cleanup");
            return;
        }
        sessionRepository.deleteAll(expired);
        LOGGER.info("Cleanup completed: {} expired generation audit session(s) deleted", expired.size());
    }
}
