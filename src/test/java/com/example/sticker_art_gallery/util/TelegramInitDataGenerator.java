package com.example.sticker_art_gallery.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Утилита для генерации валидной Telegram initData для автотестов
 * 
 * Использование:
 * <pre>
 * String initData = TelegramInitDataGenerator.builder()
 *     .botToken("your_bot_token")
 *     .userId(123456789L)
 *     .username("testuser")
 *     .firstName("Test")
 *     .lastName("User")
 *     .build();
 * </pre>
 */
public class TelegramInitDataGenerator {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    private final String botToken;
    private final Long userId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String languageCode;
    private final Long authDate;
    private final String queryId;
    private final Map<String, String> additionalParams;
    
    private TelegramInitDataGenerator(Builder builder) {
        this.botToken = builder.botToken;
        this.userId = builder.userId;
        this.username = builder.username;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.languageCode = builder.languageCode;
        this.authDate = builder.authDate;
        this.queryId = builder.queryId;
        this.additionalParams = builder.additionalParams;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Генерирует валидную initData строку с правильной HMAC подписью
     */
    public String generate() throws Exception {
        // Формируем JSON с данными пользователя
        String userJson = buildUserJson();
        
        // Собираем все параметры
        Map<String, String> params = new TreeMap<>();
        params.put("user", userJson);
        params.put("auth_date", String.valueOf(authDate));
        
        if (queryId != null) {
            params.put("query_id", queryId);
        }
        
        // Добавляем дополнительные параметры
        params.putAll(additionalParams);
        
        // Создаем dataCheckString (отсортированные параметры через \n)
        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        // Вычисляем HMAC подпись
        String hash = calculateHash(dataCheckString, botToken);
        
        // Добавляем hash к параметрам
        params.put("hash", hash);
        
        // Формируем финальную initData строку (URL-encoded)
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }
    
    /**
     * Формирует JSON с данными пользователя
     */
    private String buildUserJson() {
        StringBuilder json = new StringBuilder("{");
        json.append("\"id\":").append(userId);
        
        if (firstName != null) {
            json.append(",\"first_name\":\"").append(escapeJson(firstName)).append("\"");
        }
        
        if (lastName != null) {
            json.append(",\"last_name\":\"").append(escapeJson(lastName)).append("\"");
        }
        
        if (username != null) {
            json.append(",\"username\":\"").append(escapeJson(username)).append("\"");
        }
        
        if (languageCode != null) {
            json.append(",\"language_code\":\"").append(escapeJson(languageCode)).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Вычисляет HMAC-SHA256 подпись по алгоритму Telegram Web Apps
     */
    private String calculateHash(String dataCheckString, String botToken) throws Exception {
        // Шаг 1: Создаём секретный ключ: HMAC-SHA256("WebAppData", botToken)
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec botTokenKeySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(botTokenKeySpec);
        byte[] secretKey = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
        
        // Шаг 2: Вычисляем HMAC-SHA256(dataCheckString, secretKey)
        mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        
        // Шаг 3: Конвертируем в hex строку
        return bytesToHex(hashBytes);
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
    
    /**
     * URL-encode строки
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode: " + value, e);
        }
    }
    
    /**
     * Экранирует спецсимволы для JSON
     */
    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Builder для удобного создания initData
     */
    public static class Builder {
        private String botToken;
        private Long userId;
        private String username;
        private String firstName;
        private String lastName;
        private String languageCode = "ru";
        private Long authDate = Instant.now().getEpochSecond();
        private String queryId;
        private Map<String, String> additionalParams = new TreeMap<>();
        
        /**
         * Токен бота (обязательный параметр)
         */
        public Builder botToken(String botToken) {
            this.botToken = botToken;
            return this;
        }
        
        /**
         * Telegram ID пользователя (обязательный параметр)
         */
        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        /**
         * Username пользователя
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        /**
         * Имя пользователя
         */
        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }
        
        /**
         * Фамилия пользователя
         */
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        /**
         * Код языка (по умолчанию "ru")
         */
        public Builder languageCode(String languageCode) {
            this.languageCode = languageCode;
            return this;
        }
        
        /**
         * Дата авторизации в Unix timestamp (по умолчанию текущее время)
         */
        public Builder authDate(Long authDate) {
            this.authDate = authDate;
            return this;
        }
        
        /**
         * Query ID (необязательный параметр)
         */
        public Builder queryId(String queryId) {
            this.queryId = queryId;
            return this;
        }
        
        /**
         * Дополнительные параметры
         */
        public Builder additionalParam(String key, String value) {
            this.additionalParams.put(key, value);
            return this;
        }
        
        /**
         * Создает генератор и сразу генерирует initData
         */
        public String build() throws Exception {
            if (botToken == null || botToken.isEmpty()) {
                throw new IllegalArgumentException("Bot token is required");
            }
            if (userId == null) {
                throw new IllegalArgumentException("User ID is required");
            }
            
            TelegramInitDataGenerator generator = new TelegramInitDataGenerator(this);
            return generator.generate();
        }
    }
}

