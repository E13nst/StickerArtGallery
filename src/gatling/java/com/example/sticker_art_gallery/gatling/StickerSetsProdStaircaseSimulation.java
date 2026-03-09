package com.example.sticker_art_gallery.gatling;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Нагрузочный тест для эндпоинта GET /api/stickersets на проде.
 *
 * Профиль нагрузки — «лесенка по RPS»:
 *   startRps → startRps+stepRps → startRps+2*stepRps → … (steps ступеней)
 * Каждая ступень длится stepDurationSeconds секунд, между ступенями плавный ramp.
 *
 * Все параметры задаются через JVM system properties (через -D при запуске Gradle):
 *
 *   Параметр              | Дефолт | Описание
 *   ----------------------|--------|--------------------------------------------------
 *   baseUrl               | https://stickerartgallery-e13nst.amvera.io
 *   startRps              | 1      | С какого RPS стартуем (первая ступень)
 *   stepRps               | 1      | Шаг прироста RPS на каждой ступени
 *   steps                 | 20     | Количество ступеней (итоговый макс = startRps + steps*stepRps)
 *   stepDurationSeconds   | 30     | Сколько секунд держать каждую ступень
 *   rampSeconds           | 10     | Сколько секунд занимает разгон между ступенями
 *   requestTimeoutMs      | 5000   | Таймаут запроса (после которого Gatling пишет KO)
 *
 * Запуск (дефолты — 20 ступеней по 30 сек, итого ~13 минут):
 *   ./gradlew gatlingRun --non-interactive
 *
 * Быстрый тест (5 ступеней по 10 сек):
 *   ./gradlew gatlingRun --non-interactive -DstartRps=1 -DstepRps=1 -Dsteps=5 -DstepDurationSeconds=10 -DrampSeconds=5
 *
 * Отчёт появится в: build/reports/gatling/<имя симуляции>-<timestamp>/index.html
 */
public class StickerSetsProdStaircaseSimulation extends Simulation {

    private static final String BASE_URL =
            System.getProperty("baseUrl", "https://stickerartgallery-e13nst.amvera.io");

    private static final double START_RPS =
            Double.parseDouble(System.getProperty("startRps", "1"));

    private static final double STEP_RPS =
            Double.parseDouble(System.getProperty("stepRps", "1"));

    private static final int STEPS =
            Integer.parseInt(System.getProperty("steps", "20"));

    private static final int STEP_DURATION_SECONDS =
            Integer.parseInt(System.getProperty("stepDurationSeconds", "30"));

    private static final int RAMP_SECONDS =
            Integer.parseInt(System.getProperty("rampSeconds", "10"));

    private static final int REQUEST_TIMEOUT_MS =
            Integer.parseInt(System.getProperty("requestTimeoutMs", "5000"));

    // ---------------------------------------------------------------------------
    // HTTP протокол
    // ---------------------------------------------------------------------------

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling-LoadTest/1.0")
            // переиспользуем HTTP-соединения (keep-alive)
            .shareConnections();

    // ---------------------------------------------------------------------------
    // Сценарий: один запрос на виртуального пользователя
    // При открытой модели (constantUsersPerSec / incrementUsersPerSec) каждый VU
    // запускается заново → users/sec ≈ RPS
    // ---------------------------------------------------------------------------

    private final ScenarioBuilder stickerSetsScenario = scenario("Stickersets gallery — staircase load")
            .exec(
                    http("GET /api/stickersets")
                            .get("/api/stickersets")
                            .requestTimeout(Duration.ofMillis(REQUEST_TIMEOUT_MS))
                            .queryParam("page", "0")
                            .queryParam("size", "20")
                            .queryParam("sort", "likesCount")
                            .queryParam("direction", "DESC")
                            .queryParam("officialOnly", "false")
                            .queryParam("isVerified", "false")
                            .queryParam("likedOnly", "false")
                            .queryParam("shortInfo", "false")
                            .queryParam("preview", "false")
                            // OK = HTTP 200 и в теле есть поле "content" (пагинация)
                            .check(status().is(200))
                            .check(jsonPath("$.content").exists())
            );

    // ---------------------------------------------------------------------------
    // Профиль нагрузки: ступенчатый рост RPS
    //
    // incrementUsersPerSec(STEP_RPS)  — на каждой ступени добавляется STEP_RPS пользователей/сек
    //   .times(STEPS)                 — STEPS ступеней
    //   .eachLevelLasting(...)        — каждая ступень длится STEP_DURATION_SECONDS сек
    //   .separatedByRampsLasting(...) — между ступенями разгон RAMP_SECONDS сек
    //   .startingFrom(START_RPS)      — первая ступень = START_RPS пользователей/сек
    //
    // Итоговый максимальный RPS = START_RPS + STEPS * STEP_RPS
    // Общее время теста ≈ STEPS * (STEP_DURATION_SECONDS + RAMP_SECONDS) секунд
    // ---------------------------------------------------------------------------

    {
        double maxRps = START_RPS + (double) STEPS * STEP_RPS;
        double totalMinutes = (double) STEPS * (STEP_DURATION_SECONDS + RAMP_SECONDS) / 60.0;

        System.out.printf(
                "%n╔══════════════════════════════════════════════════╗%n" +
                "║        GATLING STAIRCASE LOAD TEST               ║%n" +
                "╠══════════════════════════════════════════════════╣%n" +
                "║  Target:   %-37s ║%n" +
                "║  Steps:    %-2d ступени, %d сек каждая + %d сек ramp  ║%n" +
                "║  RPS:      %.0f → %.0f (шаг +%.0f RPS)             ║%n" +
                "║  Duration: ~%.1f минут                            ║%n" +
                "╚══════════════════════════════════════════════════╝%n",
                BASE_URL,
                STEPS, STEP_DURATION_SECONDS, RAMP_SECONDS,
                START_RPS, maxRps, STEP_RPS,
                totalMinutes
        );

        setUp(
                stickerSetsScenario.injectOpen(
                        incrementUsersPerSec(STEP_RPS)
                                .times(STEPS)
                                .eachLevelLasting(Duration.ofSeconds(STEP_DURATION_SECONDS))
                                .separatedByRampsLasting(Duration.ofSeconds(RAMP_SECONDS))
                                .startingFrom(START_RPS)
                )
        ).protocols(httpProtocol);
    }
}
