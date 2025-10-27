package com.example.sticker_art_gallery.benchmark;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.model.profile.UserProfileRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Бенчмарк-тест для измерения производительности загрузки галереи
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Epic("Производительность")
@Feature("Бенчмарк галереи и загрузки стикеров")
@DisplayName("Бенчмарк: загрузка галереи")
class GalleryLoadBenchmarkTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GalleryLoadBenchmarkTest.class);
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private StickerSetRepository stickerSetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    private StickerSetTestSteps testSteps;
    private String validInitData;
    
    private static final int PAGE = 0;
    private static final int SIZE = 20;
    private static final int STICKERS_PER_SET = 4;
    private static final int THREAD_POOL_SIZE = 20; // Параллельная загрузка
    private static final int TIMEOUT_SECONDS = 60; // Таймаут для загрузки файлов
    
    @BeforeEach
    void setUp() throws Exception {
        testSteps = new StickerSetTestSteps();
        testSteps.setMockMvc(mockMvc);
        testSteps.setObjectMapper(objectMapper);
        testSteps.setAppConfig(appConfig);
        testSteps.setStickerSetRepository(stickerSetRepository);
        testSteps.setUserRepository(userRepository);
        testSteps.setUserProfileRepository(userProfileRepository);
        
        testSteps.createTestUserAndProfile(TestDataBuilder.TEST_USER_ID);
        validInitData = testSteps.createValidInitData(TestDataBuilder.TEST_USER_ID);
    }
    
    @Test
    @Story("Производительность загрузки галереи")
    @DisplayName("📊 Бенчмарк: загрузка 20 стикерсетов и 80 файлов стикеров")
    @Description("Измеряет время загрузки галереи (20 стикерсетов) и параллельной загрузки файлов стикеров (4 из каждого сета)")
    @Severity(SeverityLevel.CRITICAL)
    void benchmarkGalleryLoadWithStickerFiles() throws Exception {
        LOGGER.info("🚀 Начало бенчмарк-теста загрузки галереи");
        
        LoadMetrics metrics = new LoadMetrics();
        long overallStartTime = System.currentTimeMillis();
        
        // ===== ШАГ 1: Загрузка списка стикерсетов =====
        Allure.step("Шаг 1: Загрузка списка стикерсетов (page=0, size=20)", () -> {
            LOGGER.info("📋 Загрузка списка стикерсетов: page={}, size={}", PAGE, SIZE);
        });
        
        long stickerSetsStartTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(get("/api/stickersets")
                        .param("page", String.valueOf(PAGE))
                        .param("size", String.valueOf(SIZE)))
                .andExpect(status().isOk())
                .andReturn();
        
        long stickerSetsLoadTime = System.currentTimeMillis() - stickerSetsStartTime;
        
        String responseBody = result.getResponse().getContentAsString();
        PageResponse<StickerSetDto> pageResponse = objectMapper.readValue(
                responseBody, 
                new TypeReference<PageResponse<StickerSetDto>>() {}
        );
        
        List<StickerSetDto> stickerSets = pageResponse.getContent();
        
        LOGGER.info("✅ Загружено {} стикерсетов за {} мс", stickerSets.size(), stickerSetsLoadTime);
        Allure.addAttachment("Время загрузки списка стикерсетов", "text/plain", 
                stickerSetsLoadTime + " мс");
        
        assertFalse(stickerSets.isEmpty(), "Список стикерсетов не должен быть пустым");
        
        // ===== ШАГ 2: Извлечение file_id из стикерсетов =====
        Allure.step("Шаг 2: Извлечение file_id стикеров из каждого сета", () -> {
            LOGGER.info("🔍 Извлечение file_id стикеров из {} стикерсетов", stickerSets.size());
        });
        
        List<String> fileIdsToLoad = extractFileIds(stickerSets, STICKERS_PER_SET);
        
        LOGGER.info("📝 Извлечено {} file_id для загрузки", fileIdsToLoad.size());
        Allure.addAttachment("Количество файлов для загрузки", "text/plain", 
                fileIdsToLoad.size() + " файлов");
        
        assertFalse(fileIdsToLoad.isEmpty(), "Должен быть хотя бы один file_id для загрузки");
        
        // ===== ШАГ 3: Параллельная загрузка файлов =====
        Allure.step("Шаг 3: Параллельная загрузка " + fileIdsToLoad.size() + " файлов стикеров", () -> {
            LOGGER.info("⚡ Параллельная загрузка {} файлов (pool size: {})", 
                    fileIdsToLoad.size(), THREAD_POOL_SIZE);
        });
        
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String fileId : fileIdsToLoad) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                loadStickerFile(fileId, metrics);
            }, executorService);
            
            futures.add(future);
        }
        
        // Ждем завершения всех загрузок
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER.error("⏱️ Превышен таймаут загрузки файлов: {} секунд", TIMEOUT_SECONDS);
            fail("Превышен таймаут загрузки файлов: " + TIMEOUT_SECONDS + " секунд");
        } finally {
            executorService.shutdown();
        }
        
        metrics.finish();
        long overallDuration = System.currentTimeMillis() - overallStartTime;
        
        LOGGER.info("🏁 Бенчмарк завершен за {} мс", overallDuration);
        
        // ===== ШАГ 4: Сбор и анализ статистики =====
        Allure.step("Шаг 4: Анализ результатов и статистика", () -> {
            LoadMetrics.Statistics stats = metrics.getStatistics();
            
            LOGGER.info(stats.toFormattedString());
            
            // Прикрепляем статистику к отчету Allure
            attachStatisticsToAllure(stats, metrics, stickerSetsLoadTime, overallDuration);
            
            // Проверка SLA (пример: 95% запросов должны быть быстрее 5000 мс)
            if (stats.getP95Ms() != null) {
                Allure.step("Проверка SLA: p95 < 5000ms", () -> {
                    assertTrue(stats.getP95Ms() < 5000, 
                            String.format("SLA нарушен: p95=%.2f мс (ожидалось < 5000 мс)", stats.getP95Ms()));
                    LOGGER.info("✅ SLA выполнен: p95={} мс", stats.getP95Ms());
                });
            }
            
            // Проверка успешности загрузки
            Allure.step("Проверка успешности загрузки (> 90%)", () -> {
                double successRate = stats.getSuccessRate();
                assertTrue(successRate > 90.0, 
                        String.format("Низкая успешность загрузки: %.1f%% (ожидалось > 90%%)", successRate));
                LOGGER.info("✅ Успешность загрузки: {}%", successRate);
            });
        });
    }
    
    /**
     * Извлекает случайные file_id из стикерсетов
     * Использует фиксированный seed для повторяемости (нужно для тестирования кеша)
     */
    private List<String> extractFileIds(List<StickerSetDto> stickerSets, int countPerSet) {
        List<String> fileIds = new ArrayList<>();
        Random random = new Random(42); // Фиксированный seed для повторяемости
        
        for (StickerSetDto stickerSet : stickerSets) {
            Object telegramInfo = stickerSet.getTelegramStickerSetInfo();
            
            if (telegramInfo == null) {
                LOGGER.warn("⚠️ StickerSet {} не имеет telegramStickerSetInfo", stickerSet.getName());
                continue;
            }
            
            try {
                // Парсим JSON с информацией о стикерсете
                @SuppressWarnings("unchecked")
                Map<String, Object> stickerSetInfo = (Map<String, Object>) telegramInfo;
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stickers = (List<Map<String, Object>>) stickerSetInfo.get("stickers");
                
                if (stickers == null || stickers.isEmpty()) {
                    LOGGER.warn("⚠️ StickerSet {} не содержит стикеров", stickerSet.getName());
                    continue;
                }
                
                // Берем случайные стикеры
                List<Map<String, Object>> shuffled = new ArrayList<>(stickers);
                Collections.shuffle(shuffled, random);
                
                int count = Math.min(countPerSet, shuffled.size());
                for (int i = 0; i < count; i++) {
                    Map<String, Object> sticker = shuffled.get(i);
                    
                    // Ищем file_id в разных полях (file_id, thumbnail, etc.)
                    String fileId = extractFileIdFromSticker(sticker);
                    if (fileId != null) {
                        fileIds.add(fileId);
                    }
                }
                
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка при извлечении file_id из стикерсета {}: {}", 
                        stickerSet.getName(), e.getMessage());
            }
        }
        
        return fileIds;
    }
    
    /**
     * Извлекает file_id из стикера (ищет в разных полях)
     */
    private String extractFileIdFromSticker(Map<String, Object> sticker) {
        // Пробуем разные поля
        String[] fileIdFields = {"file_id", "thumb", "thumbnail"};
        
        for (String field : fileIdFields) {
            Object value = sticker.get(field);
            if (value != null) {
                if (value instanceof String) {
                    return (String) value;
                } else if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedMap = (Map<String, Object>) value;
                    Object fileId = nestedMap.get("file_id");
                    if (fileId instanceof String) {
                        return (String) fileId;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Загружает файл стикера и записывает метрики
     */
    private void loadStickerFile(String fileId, LoadMetrics metrics) {
        long startTime = System.currentTimeMillis();
        
        try {
            MvcResult result = mockMvc.perform(get("/api/stickers/" + fileId))
                    .andReturn();
            
            long duration = System.currentTimeMillis() - startTime;
            int status = result.getResponse().getStatus();
            
            if (status == HttpStatus.OK.value()) {
                byte[] content = result.getResponse().getContentAsByteArray();
                metrics.recordSuccess(fileId, duration, content.length);
                LOGGER.debug("✅ Файл {} загружен за {} мс ({} байт)", fileId, duration, content.length);
            } else {
                String errorMessage = String.format("HTTP %d: %s", status, result.getResponse().getErrorMessage());
                metrics.recordFailure(fileId, duration, errorMessage);
                LOGGER.error("❌ Ошибка загрузки файла {}: {}", fileId, errorMessage);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            metrics.recordFailure(fileId, duration, errorMessage);
            LOGGER.error("❌ Исключение при загрузке файла {}: {}", fileId, e.getMessage());
        }
    }
    
    /**
     * Прикрепляет статистику к отчету Allure
     */
    private void attachStatisticsToAllure(LoadMetrics.Statistics stats, LoadMetrics metrics, 
                                         long stickerSetsLoadTime, long overallDuration) {
        // Общая статистика
        Allure.addAttachment("📊 Общая статистика", "text/plain", stats.toFormattedString());
        
        // JSON со статистикой
        try {
            String statsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats);
            Allure.addAttachment("Статистика (JSON)", "application/json", statsJson, "json");
        } catch (Exception e) {
            LOGGER.error("Ошибка при сериализации статистики в JSON", e);
        }
        
        // Топ-10 самых медленных файлов
        List<LoadMetrics.FileLoadResult> slowest = metrics.getSlowestFiles(10);
        if (!slowest.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("TOP-10 самых медленных файлов:\n\n");
            for (int i = 0; i < slowest.size(); i++) {
                LoadMetrics.FileLoadResult file = slowest.get(i);
                sb.append(String.format("%d. %s - %d мс (%s)\n", 
                        i + 1, file.getFileId(), file.getDurationMs(), file.getSpeedKbps()));
            }
            Allure.addAttachment("🐌 Топ-10 медленных файлов", "text/plain", sb.toString());
        }
        
        // Список ошибок
        List<LoadMetrics.FileLoadResult> failed = metrics.getFailedLoads();
        if (!failed.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("❌ ОШИБКИ ЗАГРУЗКИ (").append(failed.size()).append(" файлов):\n\n");
            for (LoadMetrics.FileLoadResult file : failed) {
                sb.append(String.format("File ID: %s\n", file.getFileId()));
                sb.append(String.format("Время попытки: %d мс\n", file.getDurationMs()));
                sb.append(String.format("Ошибка: %s\n", file.getErrorMessage()));
                sb.append("─────────────────────────────────────────\n");
            }
            Allure.addAttachment("❌ Ошибки загрузки", "text/plain", sb.toString());
        }
        
        // Сводная таблица
        StringBuilder summary = new StringBuilder();
        summary.append("СВОДНАЯ ИНФОРМАЦИЯ\n");
        summary.append("═══════════════════════════════════════════\n\n");
        summary.append(String.format("Загрузка списка стикерсетов:  %d мс\n", stickerSetsLoadTime));
        summary.append(String.format("Загрузка файлов стикеров:     %d мс\n", stats.getTotalDurationMs()));
        summary.append(String.format("Общее время теста:            %d мс\n", overallDuration));
        summary.append(String.format("\nВсего файлов:                 %d\n", stats.getTotalRequests()));
        summary.append(String.format("Успешно загружено:            %d (%.1f%%)\n", 
                stats.getSuccessCount(), stats.getSuccessRate()));
        summary.append(String.format("Ошибок:                       %d\n", stats.getFailureCount()));
        
        Allure.addAttachment("📋 Сводка", "text/plain", summary.toString());
    }
}

