package com.example.sticker_art_gallery.controller.internal;

import com.example.sticker_art_gallery.model.payment.StarsPackageEntity;
import com.example.sticker_art_gallery.model.payment.StarsPurchaseEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.StarsPackageRepository;
import com.example.sticker_art_gallery.repository.StarsPurchaseRepository;
import com.example.sticker_art_gallery.repository.UserRepository;
import io.qameta.allure.*;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Epic("Платежи Telegram Stars")
@Feature("Webhook от Python сервиса")
@DisplayName("Integration тесты для Telegram Stars webhook")
class TelegramWebhookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StarsPackageRepository starsPackageRepository;

    @Autowired
    private StarsPurchaseRepository starsPurchaseRepository;

    @Value("${app.internal.service-tokens.sticker-bot:}")
    private String serviceToken;

    @Value("${app.telegram.webhook.secret:test_secret}")
    private String webhookSecret;

    private static final String WEBHOOK_URL = "/api/internal/webhooks/stars-payment";
    private static final Long TEST_USER_ID = 999999991L;
    private static final String TEST_PACKAGE_CODE = "TEST_WEBHOOK";

    private StarsPackageEntity testPackage;

    @BeforeEach
    void setUp() {
        // Очистка тестовых данных
        starsPurchaseRepository.findByTelegramChargeId("test_charge_webhook_001")
                .ifPresent(starsPurchaseRepository::delete);

        // Создание тестового пользователя (если не существует)
        userRepository.findById(TEST_USER_ID).orElseGet(() -> {
            UserEntity user = new UserEntity();
            user.setId(TEST_USER_ID);
            user.setUsername("test_webhook_user");
            user.setFirstName("Test");
            user.setLanguageCode("en");
            return userRepository.save(user);
        });

        // Создание тестового пакета
        testPackage = starsPackageRepository.findByCodeAndIsEnabledTrue(TEST_PACKAGE_CODE)
                .orElseGet(() -> {
                    StarsPackageEntity pkg = new StarsPackageEntity();
                    pkg.setCode(TEST_PACKAGE_CODE);
                    pkg.setName("Test Webhook Package");
                    pkg.setDescription("Package for webhook testing");
                    pkg.setStarsPrice(50);
                    pkg.setArtAmount(100L);
                    pkg.setIsEnabled(true);
                    pkg.setSortOrder(99);
                    return starsPackageRepository.save(pkg);
                });
    }

    @AfterEach
    void tearDown() {
        // Очистка тестовых данных
        starsPurchaseRepository.findByTelegramChargeId("test_charge_webhook_001")
                .ifPresent(starsPurchaseRepository::delete);
        starsPurchaseRepository.findByTelegramChargeId("test_charge_webhook_002")
                .ifPresent(starsPurchaseRepository::delete);
    }

    @Test
    @Story("Успешная обработка webhook")
    @DisplayName("Должен успешно обработать валидный webhook платеж")
    @Description("Проверяет полный flow обработки webhook: проверка подписи, создание purchase, начисление ART")
    @Severity(SeverityLevel.BLOCKER)
    void shouldProcessValidWebhook() throws Exception {
        // Given
        String requestBody = createWebhookPayload(
                "telegram_stars_payment_succeeded",
                TEST_USER_ID,
                50,
                "XTR",
                "test_charge_webhook_001",
                String.format("{\"package_id\":%d}", testPackage.getId()),
                System.currentTimeMillis() / 1000
        );

        String signature = generateHmacSignature(requestBody);

        // When & Then
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("X-Service-Token", serviceToken)
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.purchaseId").isNumber())
                .andExpect(jsonPath("$.artCredited").value(100))
                .andExpect(jsonPath("$.errorMessage").doesNotExist());

        // Проверяем что purchase создан
        StarsPurchaseEntity purchase = starsPurchaseRepository
                .findByTelegramChargeId("test_charge_webhook_001")
                .orElseThrow();

        assertEquals(TEST_USER_ID, purchase.getUserId());
        assertEquals(50, purchase.getStarsPaid());
        assertEquals(100L, purchase.getArtCredited());
        assertEquals(TEST_PACKAGE_CODE, purchase.getPackageCode());
    }

    @Test
    @Story("Отклонение невалидной подписи")
    @DisplayName("Должен отклонить запрос с невалидной HMAC подписью")
    @Description("Проверяет защиту от подделки webhook через проверку HMAC подписи")
    @Severity(SeverityLevel.CRITICAL)
    void shouldRejectInvalidSignature() throws Exception {
        // Given
        String requestBody = createWebhookPayload(
                "telegram_stars_payment_succeeded",
                TEST_USER_ID,
                50,
                "XTR",
                "test_charge_invalid_sig",
                String.format("{\"package_id\":%d}", testPackage.getId()),
                System.currentTimeMillis() / 1000
        );

        String invalidSignature = "invalid_signature_12345";

        // When & Then
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("X-Service-Token", serviceToken)
                        .header("X-Webhook-Signature", invalidSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Story("Идемпотентность платежей")
    @DisplayName("Должен корректно обрабатывать дублирующиеся webhook запросы")
    @Description("Проверяет, что повторный webhook с тем же charge_id не создает дубликат purchase")
    @Severity(SeverityLevel.CRITICAL)
    void shouldHandleIdempotency() throws Exception {
        // Given
        String requestBody = createWebhookPayload(
                "telegram_stars_payment_succeeded",
                TEST_USER_ID,
                50,
                "XTR",
                "test_charge_webhook_002",
                String.format("{\"package_id\":%d}", testPackage.getId()),
                System.currentTimeMillis() / 1000
        );

        String signature = generateHmacSignature(requestBody);

        // When - первый запрос
        var firstResponse = mockMvc.perform(post(WEBHOOK_URL)
                        .header("X-Service-Token", serviceToken)
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String firstPurchaseId = new JSONObject(firstResponse.getResponse().getContentAsString())
                .getString("purchaseId");

        // When - второй запрос (дубликат)
        var secondResponse = mockMvc.perform(post(WEBHOOK_URL)
                        .header("X-Service-Token", serviceToken)
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String secondPurchaseId = new JSONObject(secondResponse.getResponse().getContentAsString())
                .getString("purchaseId");

        // Then - purchase_id должен быть одинаковым
        assertEquals(firstPurchaseId, secondPurchaseId, "Повторный webhook должен вернуть тот же purchaseId");

        // Проверяем что создан только один purchase
        long count = starsPurchaseRepository.findByTelegramChargeId("test_charge_webhook_002")
                .stream()
                .count();
        assertEquals(1, count, "Должен быть создан только один purchase");
    }

    @Test
    @Story("Валидация данных")
    @DisplayName("Должен отклонить webhook с невалидным package_id")
    @Description("Проверяет валидацию существования пакета")
    @Severity(SeverityLevel.NORMAL)
    void shouldRejectInvalidPackageId() throws Exception {
        // Given
        String requestBody = createWebhookPayload(
                "telegram_stars_payment_succeeded",
                TEST_USER_ID,
                50,
                "XTR",
                "test_charge_invalid_pkg",
                "{\"package_id\":99999}",  // Несуществующий package_id
                System.currentTimeMillis() / 1000
        );

        String signature = generateHmacSignature(requestBody);

        // When & Then
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("X-Service-Token", serviceToken)
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value(containsString("Пакет не найден")));
    }

    @Test
    @Story("Валидация данных")
    @DisplayName("Должен отклонить webhook с неверной суммой")
    @Description("Проверяет валидацию соответствия суммы пакету")
    @Severity(SeverityLevel.CRITICAL)
    void shouldRejectInvalidAmount() throws Exception {
        // Given
        String requestBody = createWebhookPayload(
                "telegram_stars_payment_succeeded",
                TEST_USER_ID,
                999,  // Неверная сумма (должна быть 50)
                "XTR",
                "test_charge_invalid_amount",
                String.format("{\"package_id\":%d}", testPackage.getId()),
                System.currentTimeMillis() / 1000
        );

        String signature = generateHmacSignature(requestBody);

        // When & Then
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("X-Service-Token", serviceToken)
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value(containsString("Несоответствие суммы")));
    }

    @Test
    @Story("Требование Service Token")
    @DisplayName("Должен отклонить запрос без X-Service-Token")
    @Description("Проверяет защиту internal API через service token")
    @Severity(SeverityLevel.CRITICAL)
    void shouldRejectWithoutServiceToken() throws Exception {
        // Given
        String requestBody = createWebhookPayload(
                "telegram_stars_payment_succeeded",
                TEST_USER_ID,
                50,
                "XTR",
                "test_charge_no_token",
                String.format("{\"package_id\":%d}", testPackage.getId()),
                System.currentTimeMillis() / 1000
        );

        String signature = generateHmacSignature(requestBody);

        // When & Then
        mockMvc.perform(post(WEBHOOK_URL)
                        // НЕ передаем X-Service-Token
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Создает JSON payload для webhook
     */
    private String createWebhookPayload(String event, Long userId, int amountStars,
                                        String currency, String chargeId, String invoicePayload,
                                        long timestamp) {
        JSONObject payload = new JSONObject();
        payload.put("event", event);
        payload.put("user_id", userId);
        payload.put("amount_stars", amountStars);
        payload.put("currency", currency);
        payload.put("telegram_charge_id", chargeId);
        payload.put("invoice_payload", invoicePayload);
        payload.put("timestamp", timestamp);
        return payload.toString();  // Canonical JSON (sorted keys)
    }

    /**
     * Генерирует HMAC-SHA256 подпись для webhook
     */
    private String generateHmacSignature(String requestBody) throws Exception {
        JSONObject json = new JSONObject(requestBody);
        String canonicalJson = json.toString();

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(canonicalJson.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
