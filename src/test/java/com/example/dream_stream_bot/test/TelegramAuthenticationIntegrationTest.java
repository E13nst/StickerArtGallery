package com.example.dream_stream_bot.test;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.util.TelegramInitDataValidator;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Автотесты для проверки аутентификации через Telegram initData
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
public class TelegramAuthenticationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private BotService botService;

    @Autowired
    private TelegramInitDataValidator validator;

    private static final String TEST_BOT_NAME = "StickerGallery";
    private static final Long TEST_USER_ID = 141614461L;
    private static final String TEST_USER_FIRST_NAME = "Andrey";
    private static final String TEST_USER_LAST_NAME = "Mitroshin";
    private static final String TEST_USER_USERNAME = "E13nst";
    private static final String TEST_USER_LANGUAGE_CODE = "ru";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    /**
     * Тест 1: Проверка существования бота StickerGallery в базе данных
     */
    @Test
    void testStickerGalleryBotExists() {
        System.out.println("🧪 Тест 1: Проверка существования бота StickerGallery");
        
        // Получаем всех ботов
        var bots = botService.findAll();
        System.out.println("📋 Найдено ботов в базе: " + bots.size());
        
        // Ищем бота StickerGallery
        BotEntity stickerGalleryBot = bots.stream()
                .filter(bot -> TEST_BOT_NAME.equals(bot.getName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(stickerGalleryBot, "Бот StickerGallery должен существовать в базе данных");
        System.out.println("✅ Бот StickerGallery найден: ID=" + stickerGalleryBot.getId() + 
                ", Name=" + stickerGalleryBot.getName() + 
                ", Username=" + stickerGalleryBot.getUsername());
        
        // Проверяем, что у бота есть токен
        assertNotNull(stickerGalleryBot.getToken(), "У бота должен быть токен");
        assertFalse(stickerGalleryBot.getToken().isEmpty(), "Токен бота не должен быть пустым");
        System.out.println("✅ Токен бота присутствует (длина: " + stickerGalleryBot.getToken().length() + ")");
        
        // Проверяем через API
        Response response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/bots")
                .then()
                .statusCode(401) // Должен требовать авторизацию
                .extract().response();
        
        System.out.println("✅ API /api/bots требует авторизацию (статус 401)");
    }

    /**
     * Тест 2: Генерация и валидация initData для тестового пользователя
     */
    @Test
    void testInitDataGenerationAndValidation() {
        System.out.println("🧪 Тест 2: Генерация и валидация initData");
        
        // Получаем бота
        var bots = botService.findAll();
        BotEntity stickerGalleryBot = bots.stream()
                .filter(bot -> TEST_BOT_NAME.equals(bot.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Бот StickerGallery не найден"));
        
        // Генерируем initData
        String initData = generateInitData(stickerGalleryBot.getToken());
        System.out.println("🔐 Сгенерирован initData: " + initData.substring(0, Math.min(100, initData.length())) + "...");
        
        // Валидируем initData
        boolean isValid = validator.validateInitData(initData, TEST_BOT_NAME);
        assertTrue(isValid, "InitData должен быть валидным");
        System.out.println("✅ InitData валиден");
        
        // Извлекаем telegram_id
        Long extractedTelegramId = validator.extractTelegramId(initData);
        assertEquals(TEST_USER_ID, extractedTelegramId, "Извлеченный telegram_id должен совпадать");
        System.out.println("✅ Извлечен telegram_id: " + extractedTelegramId);
    }

    /**
     * Тест 3: Полная аутентификация и получение списка стикерсетов
     */
    @Test
    void testFullAuthenticationAndStickerSetsRetrieval() {
        System.out.println("🧪 Тест 3: Полная аутентификация и получение стикерсетов");
        
        // Получаем бота
        var bots = botService.findAll();
        BotEntity stickerGalleryBot = bots.stream()
                .filter(bot -> TEST_BOT_NAME.equals(bot.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Бот StickerGallery не найден"));
        
        // Генерируем initData
        String initData = generateInitData(stickerGalleryBot.getToken());
        System.out.println("🔐 Используем initData для аутентификации");
        
        // Проверяем статус аутентификации без заголовков
        Response statusResponse = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/auth/status")
                .then()
                .statusCode(200)
                .body("authenticated", equalTo(false))
                .extract().response();
        
        System.out.println("✅ Статус без аутентификации: " + statusResponse.body().asString());
        
        // Проверяем статус аутентификации с заголовками
        Response authStatusResponse = given()
                .contentType(ContentType.JSON)
                .header("X-Telegram-Init-Data", initData)
                .header("X-Telegram-Bot-Name", TEST_BOT_NAME)
                .when()
                .get("/auth/status")
                .then()
                .statusCode(200)
                .extract().response();
        
        System.out.println("✅ Статус с аутентификацией: " + authStatusResponse.body().asString());
        
        // Получаем список стикерсетов
        Response stickerSetsResponse = given()
                .contentType(ContentType.JSON)
                .header("X-Telegram-Init-Data", initData)
                .header("X-Telegram-Bot-Name", TEST_BOT_NAME)
                .when()
                .get("/api/stickersets")
                .then()
                .statusCode(200)
                .extract().response();
        
        String responseBody = stickerSetsResponse.body().asString();
        System.out.println("✅ Получен список стикерсетов: " + responseBody);
        
        // Проверяем, что список не пустой (должен содержать стикерсеты тестового пользователя)
        assertFalse(responseBody.equals("[]"), "Список стикерсетов не должен быть пустым");
        System.out.println("✅ Список стикерсетов не пустой");
    }

    /**
     * Генерирует initData для тестового пользователя
     */
    private String generateInitData(String botToken) {
        try {
            // Создаем параметры initData
            TreeMap<String, String> params = new TreeMap<>();
            
            // Добавляем обязательные параметры
            params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
            params.put("auth_date", String.valueOf(Instant.now().getEpochSecond()));
            
            // Добавляем информацию о пользователе
            String userJson = String.format(
                "{\"id\":%d,\"first_name\":\"%s\",\"last_name\":\"%s\",\"username\":\"%s\",\"language_code\":\"%s\"}",
                TEST_USER_ID,
                TEST_USER_FIRST_NAME,
                TEST_USER_LAST_NAME,
                TEST_USER_USERNAME,
                TEST_USER_LANGUAGE_CODE
            );
            params.put("user", userJson);
            
            // Создаем строку для подписи (все параметры кроме hash, отсортированные)
            String dataCheckString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            
            // Вычисляем HMAC-SHA256 подпись согласно документации Telegram
            // Шаг 1: Создаем секретный ключ (secret_key = HMAC-SHA256(bot_token, "WebAppData"))
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec botTokenKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(botTokenKeySpec);
            byte[] secretKey = mac.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));
            
            // Шаг 2: Вычисляем hash (hash = HMAC-SHA256(data_check_string, secret_key))
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHex(hashBytes);
            
            // Добавляем hash к параметрам
            params.put("hash", hash);
            
            // Формируем финальную строку initData
            String initData = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));
            
            return initData;
            
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации initData", e);
        }
    }
    
    /**
     * Конвертирует байты в hex строку
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
