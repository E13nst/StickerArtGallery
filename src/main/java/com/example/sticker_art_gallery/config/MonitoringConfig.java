package com.example.sticker_art_gallery.config;

import com.example.sticker_art_gallery.service.memory.InMemoryChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Конфигурация мониторинга и периодической очистки для предотвращения утечек памяти
 */
@Configuration
public class MonitoringConfig {

    /**
     * Health indicator для мониторинга памяти
     */
    @Component
    public static class MemoryHealthIndicator implements HealthIndicator {
        
        @Override
        public Health health() {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usedPercentage = (double) usedMemory / maxMemory * 100;
            
            Health.Builder builder = usedPercentage < 80 ? Health.up() : Health.down();
            
            return builder
                .withDetail("used", formatBytes(usedMemory))
                .withDetail("free", formatBytes(freeMemory))
                .withDetail("total", formatBytes(totalMemory))
                .withDetail("max", formatBytes(maxMemory))
                .withDetail("usedPercentage", String.format("%.2f%%", usedPercentage))
                .build();
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            char pre = "KMGTPE".charAt(exp - 1);
            return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
        }
    }
    
    /**
     * Scheduled task для периодической очистки и логирования статистики памяти
     */
    @Component
    public static class MemoryCleanupTask {
        
        private static final Logger LOGGER = LoggerFactory.getLogger(MemoryCleanupTask.class);
        
        @Autowired(required = false)
        private InMemoryChatMemory chatMemory;
        
        /**
         * Логирование статистики памяти каждые 5 минут
         */
        @Scheduled(fixedRate = 300000) // 5 минут
        public void logMemoryStats() {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usedPercentage = (double) usedMemory / maxMemory * 100;
            
            LOGGER.info("📊 Memory Stats: Used={} MB ({:.2f}%), Free={} MB, Total={} MB, Max={} MB",
                usedMemory / 1024 / 1024,
                usedPercentage,
                freeMemory / 1024 / 1024,
                totalMemory / 1024 / 1024,
                maxMemory / 1024 / 1024);
            
            if (chatMemory != null) {
                LOGGER.info("💬 Chat Memory: {} conversations in memory", chatMemory.getConversationCount());
            }
            
            // Предупреждение при высоком использовании памяти
            if (usedPercentage > 80) {
                LOGGER.warn("⚠️ High memory usage detected: {:.2f}%. Consider investigating memory leaks.", usedPercentage);
            }
        }
        
        /**
         * Принудительная сборка мусора при критическом использовании памяти (каждые 15 минут)
         */
        @Scheduled(fixedRate = 900000) // 15 минут
        public void suggestGarbageCollection() {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usedPercentage = (double) usedMemory / maxMemory * 100;
            
            // Предложить GC только если используется более 85% памяти
            if (usedPercentage > 85) {
                LOGGER.warn("🗑️ Memory usage at {:.2f}%. Suggesting garbage collection...", usedPercentage);
                long beforeGC = usedMemory;
                System.gc();
                
                // Подождать немного для завершения GC
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                long afterGC = runtime.totalMemory() - runtime.freeMemory();
                long freed = beforeGC - afterGC;
                
                if (freed > 0) {
                    LOGGER.info("✅ GC freed {} MB of memory", freed / 1024 / 1024);
                }
            }
        }
    }
}
