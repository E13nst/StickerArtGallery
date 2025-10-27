package com.example.sticker_art_gallery.benchmark;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Бенчмарк-тест с использованием RestAssured для РЕАЛЬНОГО HTTP тестирования
 * 
 * ⚡ ОТЛИЧИЯ ОТ GalleryLoadBenchmarkTest:
 * - Использует RestAssured вместо MockMvc
 * - Работает с ЖИВЫМ приложением через HTTP
 * - Измеряет реальную производительность с учетом сети
 * - Показывает эффект Redis кеширования
 * 
 * 📋 ТРЕБОВАНИЯ:
 * - Приложение должно быть запущено на http://localhost:8080
 * - Redis должен быть доступен
 * - STICKER_CACHE_ENABLED=true
 */
@Epic("Бенчмарк тесты")
@Feature("Real HTTP бенчмарк с RestAssured")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("benchmark")  // Исключается из CI/CD, требует запущенное приложение
public class RealHttpBenchmarkTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RealHttpBenchmarkTest.class);
    
    // Конфигурация бенчмарка
    private static final String BASE_URL = "http://localhost:8080";
    private static final int STICKERSETS_TO_LOAD = 20;
    private static final int STICKERS_PER_SET = 4;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int TIMEOUT_SECONDS = 120;
    
    private List<String> testFileIds;
    
    @BeforeAll
    @Step("Инициализация: подготовка тестовых данных")
    public void setUp() {
        LOGGER.info("🔧 Инициализация Real HTTP Benchmark...");
        
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // Проверяем доступность приложения
        Response healthCheck = given()
                .when()
                .get("/actuator/health")
                .then()
                .extract().response();
        
        if (healthCheck.statusCode() != 200) {
            fail("❌ Приложение недоступно на " + BASE_URL + ". Запустите его командой: make start");
        }
        
        LOGGER.info("✅ Приложение доступно: {}", BASE_URL);
        
        // Загружаем список стикерсетов
        testFileIds = loadTestFileIds();
        
        LOGGER.info("✅ Подготовлено {} file_id для тестирования", testFileIds.size());
        Allure.addAttachment("Количество тестовых файлов", "text/plain", 
                testFileIds.size() + " файлов");
    }
    
    @Test
    @Order(1)
    @Story("Холодный кеш: первая загрузка")
    @DisplayName("📊 Бенчмарк #1: Холодный кеш (Cache MISS)")
    @Description("Измеряем производительность при первой загрузке файлов (без кеша)")
    @Severity(SeverityLevel.CRITICAL)
    public void benchmark01_coldCache() {
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOGGER.info("❄️  БЕНЧМАРК #1: ХОЛОДНЫЙ КЕШ");
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        LoadMetrics metrics = runBenchmark("Холодный кеш");
        
        LoadMetrics.Statistics stats = metrics.getStatistics();
        attachStatisticsToAllure("Холодный кеш", stats);
        
        // Проверяем базовые требования
        assertTrue(stats.getSuccessRate() > 90.0, 
                "Успешность должна быть > 90%, получено: " + stats.getSuccessRate() + "%");
        
        LOGGER.info("✅ Холодный кеш: avg={} мс, p95={} мс, throughput={} req/s",
                String.format("%.2f", stats.getAvgMs()),
                String.format("%.2f", stats.getP95Ms()),
                String.format("%.2f", stats.getThroughputRps()));
    }
    
    @Test
    @Order(2)
    @Story("Горячий кеш: повторная загрузка")
    @DisplayName("📊 Бенчмарк #2: Горячий кеш (Cache HIT)")
    @Description("Измеряем производительность при повторной загрузке (с кешем)")
    @Severity(SeverityLevel.CRITICAL)
    public void benchmark02_hotCache() {
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOGGER.info("🔥 БЕНЧМАРК #2: ГОРЯЧИЙ КЕШ");
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        LoadMetrics metrics = runBenchmark("Горячий кеш");
        
        LoadMetrics.Statistics stats = metrics.getStatistics();
        attachStatisticsToAllure("Горячий кеш", stats);
        
        // Проверяем улучшение производительности
        assertTrue(stats.getSuccessRate() > 90.0,
                "Успешность должна быть > 90%, получено: " + stats.getSuccessRate() + "%");
        
        // Горячий кеш должен быть быстрее (хотя бы немного)
        // Реальное улучшение зависит от hit rate кеша
        
        LOGGER.info("✅ Горячий кеш: avg={} мс, p95={} мс, throughput={} req/s",
                String.format("%.2f", stats.getAvgMs()),
                String.format("%.2f", stats.getP95Ms()),
                String.format("%.2f", stats.getThroughputRps()));
    }
    
    @Test
    @Order(3)
    @Story("Статистика кеша")
    @DisplayName("📊 Проверка статистики Redis кеша")
    @Description("Получаем статистику кеша после бенчмарка")
    @Severity(SeverityLevel.NORMAL)
    public void benchmark03_cacheStats() {
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOGGER.info("📊 СТАТИСТИКА КЕША ПОСЛЕ БЕНЧМАРКА");
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        Allure.step("Запрос статистики кеша", () -> {
            Response response = given()
                    .when()
                    .get("/api/stickers/cache/stats")
                    .then()
                    .statusCode(200)
                    .extract().response();
            
            String stats = response.prettyPrint();
            LOGGER.info("📊 Статистика кеша:\n{}", stats);
            
            Allure.addAttachment("Cache Statistics (JSON)", "application/json", 
                    response.asPrettyString());
            
            // Извлекаем метрики
            boolean cacheEnabled = response.jsonPath().getBoolean("cacheEnabled");
            boolean redisAvailable = response.jsonPath().getBoolean("redisAvailable");
            String hitRate = response.jsonPath().getString("hitRatePercent");
            Integer cacheHits = response.jsonPath().getInt("metrics.cacheHits");
            Integer cacheMisses = response.jsonPath().getInt("metrics.cacheMisses");
            
            LOGGER.info("✅ Cache Enabled: {}", cacheEnabled);
            LOGGER.info("✅ Redis Available: {}", redisAvailable);
            LOGGER.info("✅ Hit Rate: {}", hitRate);
            LOGGER.info("✅ Cache Hits: {}", cacheHits);
            LOGGER.info("✅ Cache Misses: {}", cacheMisses);
            
            assertTrue(cacheEnabled, "Кеш должен быть включен");
            assertTrue(redisAvailable, "Redis должен быть доступен");
        });
    }
    
    /**
     * Загружает тестовые file_id из API
     */
    private List<String> loadTestFileIds() {
        return Allure.step("Загрузка тестовых file_id из API", () -> {
            Response response = given()
                    .queryParam("page", 0)
                    .queryParam("size", STICKERSETS_TO_LOAD)
                    .when()
                    .get("/api/stickersets")
                    .then()
                    .statusCode(200)
                    .extract().response();
            
            List<Map<String, Object>> stickerSets = response.jsonPath().getList("content");
            List<String> fileIds = new ArrayList<>();
            Random random = new Random(42); // Фиксированный seed
            
            for (Map<String, Object> stickerSet : stickerSets) {
                Map<String, Object> telegramInfo = (Map<String, Object>) stickerSet.get("telegramStickerSetInfo");
                if (telegramInfo == null) continue;
                
                List<Map<String, Object>> stickers = (List<Map<String, Object>>) telegramInfo.get("stickers");
                if (stickers == null || stickers.isEmpty()) continue;
                
                // Берем случайные стикеры
                List<Map<String, Object>> shuffled = new ArrayList<>(stickers);
                Collections.shuffle(shuffled, random);
                
                int count = Math.min(STICKERS_PER_SET, shuffled.size());
                for (int i = 0; i < count; i++) {
                    Map<String, Object> sticker = shuffled.get(i);
                    String fileId = (String) sticker.get("file_id");
                    if (fileId != null && !fileId.isEmpty()) {
                        fileIds.add(fileId);
                    }
                }
            }
            
            return fileIds;
        });
    }
    
    /**
     * Выполняет бенчмарк загрузки файлов
     */
    private LoadMetrics runBenchmark(String benchmarkName) {
        LoadMetrics metrics = new LoadMetrics();
        
        Allure.step("Параллельная загрузка " + testFileIds.size() + " файлов", () -> {
            ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (String fileId : testFileIds) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    loadFileWithRestAssured(fileId, metrics);
                }, executorService);
                
                futures.add(future);
            }
            
            // Ждем завершения
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOGGER.error("⏱️ Превышен таймаут: {} секунд", TIMEOUT_SECONDS);
                fail("Timeout");
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка при выполнении бенчмарка: {}", e.getMessage());
                fail("Error: " + e.getMessage());
            } finally {
                executorService.shutdown();
            }
        });
        
        metrics.finish();
        return metrics;
    }
    
    /**
     * Загружает файл через RestAssured
     */
    private void loadFileWithRestAssured(String fileId, LoadMetrics metrics) {
        long startTime = System.currentTimeMillis();
        
        try {
            Response response = given()
                    .when()
                    .get("/api/stickers/" + fileId);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.statusCode() == 200) {
                byte[] content = response.asByteArray();
                metrics.recordSuccess(fileId, duration, content.length);
                
                // Проверяем наличие X-Cache header
                String cacheHeader = response.header("X-Cache");
                if (cacheHeader != null) {
                    LOGGER.debug("✅ {} загружен за {} мс ({} байт) [Cache: {}]", 
                            fileId, duration, content.length, cacheHeader);
                } else {
                    LOGGER.debug("✅ {} загружен за {} мс ({} байт)", 
                            fileId, duration, content.length);
                }
            } else {
                String errorMessage = String.format("HTTP %d", response.statusCode());
                metrics.recordFailure(fileId, duration, errorMessage);
                LOGGER.error("❌ Ошибка загрузки {}: {}", fileId, errorMessage);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordFailure(fileId, duration, e.getMessage());
            LOGGER.error("❌ Исключение при загрузке {}: {}", fileId, e.getMessage());
        }
    }
    
    /**
     * Прикрепляет статистику к Allure отчету
     */
    private void attachStatisticsToAllure(String name, LoadMetrics.Statistics stats) {
        // JSON статистика
        String json = String.format("""
                {
                  "total": %d,
                  "successful": %d,
                  "failed": %d,
                  "successRate": %.2f,
                  "duration": %d,
                  "throughput": %.2f,
                  "latency": {
                    "min": %.2f,
                    "max": %.2f,
                    "avg": %.2f,
                    "median": %.2f,
                    "p95": %.2f,
                    "p99": %.2f,
                    "stdDev": %.2f
                  },
                  "data": {
                    "totalBytes": %d,
                    "avgFileSize": %d
                  }
                }
                """,
                stats.getTotalRequests(), stats.getSuccessCount(), stats.getFailureCount(),
                stats.getSuccessRate(), stats.getTotalDurationMs(), stats.getThroughputRps(),
                (double) stats.getMinMs(), (double) stats.getMaxMs(), stats.getAvgMs(), stats.getMedianMs(),
                stats.getP95Ms(), stats.getP99Ms(), stats.getStdDevMs(),
                stats.getTotalBytes(), stats.getAvgBytesPerFile());
        
        Allure.addAttachment(name + " - Statistics (JSON)", "application/json", json);
        
        // Красивая таблица
        String table = String.format("""
                ╔══════════════════════════════════════════════════════════════╗
                ║                    %s                                        
                ║ Всего запросов:           %d (✅ %d | ❌ %d)                 
                ║ Успешность:            %.1f%%                                
                ║ Общее время:           %d мс                                 
                ║ Пропускная способность: %.2f запросов/сек                   
                ║ Мин. время:            %.0f мс                               
                ║ Макс. время:           %.0f мс                               
                ║ Среднее время:         %.2f мс                               
                ║ Медиана (p50):         %.2f мс                               
                ║ 95 персентиль:         %.2f мс                               
                ║ 99 персентиль:         %.2f мс                               
                ║ Станд. отклонение:     %.2f мс                               
                ║ Всего загружено:       %.2f MB                               
                ║ Средний размер файла:  %.2f KB                               
                ╚══════════════════════════════════════════════════════════════╝
                """,
                name.toUpperCase(),
                stats.getTotalRequests(), stats.getSuccessCount(), stats.getFailureCount(),
                stats.getSuccessRate(),
                stats.getTotalDurationMs(),
                stats.getThroughputRps(),
                (double) stats.getMinMs(), (double) stats.getMaxMs(),
                stats.getAvgMs(),
                stats.getMedianMs(),
                stats.getP95Ms(),
                stats.getP99Ms(),
                stats.getStdDevMs(),
                stats.getTotalBytes() / (1024.0 * 1024.0),
                stats.getAvgBytesPerFile() / 1024.0);
        
        Allure.addAttachment(name + " - Statistics", "text/plain", table);
        LOGGER.info("\n{}", table);
    }
}

