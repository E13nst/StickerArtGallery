package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class GenerationTaskCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationTaskCleanupService.class);

    private final GenerationTaskRepository taskRepository;

    @Autowired
    public GenerationTaskCleanupService(GenerationTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?") // Каждый день в 2:00 ночи
    @Transactional
    public void cleanupExpiredTasks() {
        LOGGER.info("Starting cleanup of expired generation tasks");
        
        OffsetDateTime now = OffsetDateTime.now();
        List<GenerationTaskEntity> expiredTasks = taskRepository.findByExpiresAtBefore(now);
        
        if (expiredTasks.isEmpty()) {
            LOGGER.info("No expired tasks to cleanup");
            return;
        }
        
        int deletedCount = 0;
        for (GenerationTaskEntity task : expiredTasks) {
            // Удаляем только завершенные или провалившиеся задачи
            if (task.getStatus() == com.example.sticker_art_gallery.model.generation.GenerationTaskStatus.COMPLETED ||
                task.getStatus() == com.example.sticker_art_gallery.model.generation.GenerationTaskStatus.FAILED) {
                taskRepository.delete(task);
                deletedCount++;
            }
        }
        
        LOGGER.info("Cleanup completed: {} expired tasks deleted out of {} found", deletedCount, expiredTasks.size());
    }
}
