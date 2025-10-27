package com.example.sticker_art_gallery.benchmark;

import lombok.Data;
import lombok.Builder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс для сбора метрик производительности загрузки файлов
 */
public class LoadMetrics {
    
    private final List<FileLoadResult> results = new CopyOnWriteArrayList<>();
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong endTime = new AtomicLong(0);
    
    // Для отслеживания ошибок по файлам
    private final Map<String, String> failedFiles = new ConcurrentHashMap<>();
    
    /**
     * Записать успешную загрузку файла
     */
    public void recordSuccess(String fileId, long durationMs, long bytes) {
        results.add(FileLoadResult.builder()
                .fileId(fileId)
                .durationMs(durationMs)
                .bytes(bytes)
                .success(true)
                .build());
        successCount.incrementAndGet();
        totalBytes.addAndGet(bytes);
    }
    
    /**
     * Записать неудачную загрузку файла
     */
    public void recordFailure(String fileId, long durationMs, String errorMessage) {
        results.add(FileLoadResult.builder()
                .fileId(fileId)
                .durationMs(durationMs)
                .bytes(0)
                .success(false)
                .errorMessage(errorMessage)
                .build());
        failureCount.incrementAndGet();
        failedFiles.put(fileId, errorMessage);
    }
    
    /**
     * Установить время окончания теста
     */
    public void finish() {
        endTime.set(System.currentTimeMillis());
    }
    
    /**
     * Получить общую статистику
     */
    public Statistics getStatistics() {
        List<Long> successDurations = results.stream()
                .filter(FileLoadResult::isSuccess)
                .map(FileLoadResult::getDurationMs)
                .sorted()
                .toList();
        
        if (successDurations.isEmpty()) {
            return Statistics.builder()
                    .totalRequests(results.size())
                    .successCount(0)
                    .failureCount(failureCount.get())
                    .totalDurationMs(endTime.get() - startTime.get())
                    .build();
        }
        
        long totalDuration = endTime.get() - startTime.get();
        double avgDuration = successDurations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        // Вычисляем стандартное отклонение
        double variance = successDurations.stream()
                .mapToDouble(d -> Math.pow(d - avgDuration, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        return Statistics.builder()
                .totalRequests(results.size())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .minMs(successDurations.get(0))
                .maxMs(successDurations.get(successDurations.size() - 1))
                .avgMs(avgDuration)
                .medianMs(percentile(successDurations, 50))
                .p95Ms(percentile(successDurations, 95))
                .p99Ms(percentile(successDurations, 99))
                .stdDevMs(stdDev)
                .totalBytes(totalBytes.get())
                .avgBytesPerFile(totalBytes.get() / successCount.get())
                .totalDurationMs(totalDuration)
                .throughputRps(totalDuration > 0 ? (successCount.get() * 1000.0 / totalDuration) : 0)
                .failedFiles(new HashMap<>(failedFiles))
                .build();
    }
    
    /**
     * Получить топ-N самых медленных файлов
     */
    public List<FileLoadResult> getSlowestFiles(int count) {
        return results.stream()
                .filter(FileLoadResult::isSuccess)
                .sorted(Comparator.comparingLong(FileLoadResult::getDurationMs).reversed())
                .limit(count)
                .toList();
    }
    
    /**
     * Получить список всех неудачных загрузок
     */
    public List<FileLoadResult> getFailedLoads() {
        return results.stream()
                .filter(r -> !r.isSuccess())
                .toList();
    }
    
    /**
     * Вычислить персентиль
     */
    private double percentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        
        int index = (int) Math.ceil(sortedValues.size() * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
    
    /**
     * Результат загрузки одного файла
     */
    @Data
    @Builder
    public static class FileLoadResult {
        private String fileId;
        private long durationMs;
        private long bytes;
        private boolean success;
        private String errorMessage;
        
        public String getSpeedKbps() {
            if (!success || durationMs == 0) {
                return "N/A";
            }
            double kbps = (bytes / 1024.0) / (durationMs / 1000.0);
            return String.format("%.2f KB/s", kbps);
        }
    }
    
    /**
     * Агрегированная статистика
     */
    @Data
    @Builder
    public static class Statistics {
        private int totalRequests;
        private int successCount;
        private int failureCount;
        
        private Long minMs;
        private Long maxMs;
        private Double avgMs;
        private Double medianMs;
        private Double p95Ms;
        private Double p99Ms;
        private Double stdDevMs;
        
        private long totalBytes;
        private long avgBytesPerFile;
        
        private long totalDurationMs;
        private double throughputRps;
        
        private Map<String, String> failedFiles;
        
        public double getSuccessRate() {
            return totalRequests > 0 ? (successCount * 100.0 / totalRequests) : 0.0;
        }
        
        public String getTotalBytesFormatted() {
            if (totalBytes < 1024) {
                return totalBytes + " B";
            } else if (totalBytes < 1024 * 1024) {
                return String.format("%.2f KB", totalBytes / 1024.0);
            } else {
                return String.format("%.2f MB", totalBytes / (1024.0 * 1024.0));
            }
        }
        
        public String getAvgBytesFormatted() {
            if (avgBytesPerFile < 1024) {
                return avgBytesPerFile + " B";
            } else {
                return String.format("%.2f KB", avgBytesPerFile / 1024.0);
            }
        }
        
        /**
         * Форматированный вывод статистики
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n╔══════════════════════════════════════════════════════════════╗\n");
            sb.append("║                    СТАТИСТИКА ЗАГРУЗКИ                       ║\n");
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Всего запросов:        %5d (✅ %d | ❌ %d)                  ║\n", 
                    totalRequests, successCount, failureCount));
            sb.append(String.format("║ Успешность:            %.1f%%                                ║\n", getSuccessRate()));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Общее время:           %d мс                              ║\n", totalDurationMs));
            sb.append(String.format("║ Пропускная способность: %.2f запросов/сек                  ║\n", throughputRps));
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            
            if (minMs != null) {
                sb.append(String.format("║ Мин. время:            %d мс                               ║\n", minMs));
                sb.append(String.format("║ Макс. время:           %d мс                               ║\n", maxMs));
                sb.append(String.format("║ Среднее время:         %.2f мс                             ║\n", avgMs));
                sb.append(String.format("║ Медиана (p50):         %.2f мс                             ║\n", medianMs));
                sb.append(String.format("║ 95 персентиль:         %.2f мс                             ║\n", p95Ms));
                sb.append(String.format("║ 99 персентиль:         %.2f мс                             ║\n", p99Ms));
                sb.append(String.format("║ Станд. отклонение:     %.2f мс                             ║\n", stdDevMs));
            }
            
            sb.append("╠══════════════════════════════════════════════════════════════╣\n");
            sb.append(String.format("║ Всего загружено:       %s                                 ║\n", getTotalBytesFormatted()));
            sb.append(String.format("║ Средний размер файла:  %s                                 ║\n", getAvgBytesFormatted()));
            
            if (failureCount > 0) {
                sb.append("╠══════════════════════════════════════════════════════════════╣\n");
                sb.append(String.format("║ ❌ ОШИБКИ (%d):                                            ║\n", failureCount));
                failedFiles.forEach((fileId, error) -> {
                    sb.append(String.format("║   - %s                                       ║\n", 
                            fileId.substring(0, Math.min(15, fileId.length()))));
                    sb.append(String.format("║     Причина: %s                              ║\n", 
                            error.substring(0, Math.min(40, error.length()))));
                });
            }
            
            sb.append("╚══════════════════════════════════════════════════════════════╝");
            return sb.toString();
        }
    }
}

