package com.example.sticker_art_gallery.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.sticker_art_gallery.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class TelegramInitDataValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramInitDataValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long MAX_AUTH_AGE_SECONDS = 86400; // 24 часа как в JavaScript коде

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public TelegramInitDataValidator(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    public boolean validateInitData(String initData) {
        LOGGER.debug("🔍 Начинаем валидацию initData");
        
        if (initData == null || initData.trim().isEmpty()) {
            LOGGER.warn("❌ InitData пустая или null");
            return false;
        }

        try {
            // Получаем токен бота из базы данных
            String botToken = getBotToken();
            if (botToken == null) {
                LOGGER.warn("❌ Токен бота не найден");
                return false;
            }
            LOGGER.debug("✅ Токен бота получен (длина: {})", botToken.length());
            
            // Подробное логирование входных данных
            LOGGER.debug("Полный initData: {}", initData);
            
            // Парсим параметры
            Map<String, String> params = parseInitData(initData);
            
            // Проверяем наличие обязательных полей
            if (!params.containsKey("auth_date")) {
                LOGGER.warn("❌ Отсутствует обязательное поле auth_date");
                return false;
            }
            
            String hash = params.get("hash");
            String signature = params.get("signature");
            String authDateStr = params.get("auth_date");
            
            if (hash == null && signature == null) {
                LOGGER.warn("❌ Отсутствуют поля подписи (hash или signature)");
                return false;
            }
            
            // Проверяем время auth_date
            if (!validateAuthDate(authDateStr)) {
                return false;
            }
            
            // Проверяем подпись (поддерживаем оба формата)
            boolean signatureValid = false;
            if (hash != null) {
                signatureValid = validateHash(params, hash, botToken);
                if (!signatureValid) {
                    LOGGER.warn("❌ Неверная подпись hash");
                }
            } else if (signature != null) {
                signatureValid = validateSignature(params, signature, botToken);
                if (!signatureValid) {
                    LOGGER.warn("❌ Неверная подпись signature");
                }
            }
            
            if (signatureValid) {
                LOGGER.debug("✅ InitData валидна");
            }
            
            return signatureValid;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка валидации initData: {}", e.getMessage(), e);
            return false;
        }
    }

    private String getBotToken() {
        try {
            String botToken = appConfig.getTelegram().getBotToken();
            if (botToken == null || botToken.trim().isEmpty()) {
                LOGGER.error("❌ Токен бота не настроен в конфигурации");
                return null;
            }
            LOGGER.debug("✅ Токен бота получен из конфигурации (длина: {})", botToken.length());
            return botToken;
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка получения токена бота: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> parseInitData(String initData) {
        LOGGER.debug("🔍 Парсим initData: {}", initData);
        
        Map<String, String> params = Arrays.stream(initData.split("&"))
                .map(param -> param.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> {
                            try {
                                // URL-декодируем значение параметра
                                String decodedValue = java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                                LOGGER.debug("🔍 Параметр {}: '{}' -> '{}'", parts[0], parts[1], decodedValue);
                                return decodedValue;
                            } catch (Exception e) {
                                LOGGER.warn("⚠️ Ошибка URL-декодирования параметра {}: {}", parts[0], e.getMessage());
                                return parts[1]; // Возвращаем исходное значение если декодирование не удалось
                            }
                        },
                        (existing, replacement) -> existing,
                        TreeMap::new
                ));
        
        LOGGER.debug("🔍 Результат парсинга: {}", params);
        return params;
    }

    private boolean validateAuthDate(String authDateStr) {
        try {
            long authDate = Long.parseLong(authDateStr);
            long currentTime = Instant.now().getEpochSecond();
            long ageSeconds = currentTime - authDate;
            
            if (ageSeconds > MAX_AUTH_AGE_SECONDS) {
                // Вычисляем возраст в часах и днях для более информативного сообщения
                long ageHours = ageSeconds / 3600;
                long ageDays = ageHours / 24;
                long maxAgeHours = MAX_AUTH_AGE_SECONDS / 3600;
                
                String ageDescription;
                if (ageDays > 0) {
                    ageDescription = String.format("%d дн. %d ч.", ageDays, ageHours % 24);
                } else {
                    ageDescription = String.format("%d ч.", ageHours);
                }
                
                LOGGER.warn("⚠️ Auth date устарела: initData была создана {} назад (максимум {} ч.), требуется обновление", 
                        ageDescription, maxAgeHours);
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            LOGGER.error("❌ Некорректный формат auth_date: {}", authDateStr);
            return false;
        }
    }

    /**
     * Правильное создание dataCheckString согласно документации Telegram
     * Параметры должны быть отсортированы в лексикографическом порядке
     */
    private String buildDataCheckString(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> !"hash".equals(entry.getKey()) && !"signature".equals(entry.getKey()))  // Исключаем hash и signature
                .sorted(Map.Entry.comparingByKey())  // Лексикографическая сортировка
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Исправленная версия validateHash по алгоритму JavaScript
     */
    private boolean validateHash(Map<String, String> params, String expectedHash, String botToken) {
        try {
            LOGGER.debug("🔍 Начинаем валидацию hash по алгоритму JavaScript");
            LOGGER.debug("🔍 Полученные параметры: {}", params);
            LOGGER.debug("🔍 Ожидаемый hash: {}", expectedHash);
            LOGGER.debug("🔍 Токен бота (первые 10 символов): {}", botToken.substring(0, Math.min(10, botToken.length())));
            
            // Создаём массив пар ключ-значение, исключая параметр 'hash'
            java.util.List<String> dataCheckEntries = new java.util.ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!"hash".equals(entry.getKey())) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    LOGGER.debug("🔍 Добавляем параметр: {}={}", key, value);
                    dataCheckEntries.add(key + "=" + value);
                }
            }
            
            // Сортируем пары в алфавитном порядке по ключу
            dataCheckEntries.sort(String::compareTo);
            LOGGER.debug("🔍 Отсортированные параметры: {}", dataCheckEntries);
            
            // Формируем строку для проверки
            String dataCheckString = String.join("\n", dataCheckEntries);
            LOGGER.debug("🔍 Data check string: {}", dataCheckString.replace("\n", "\\n"));
            
            // Создаём секретный ключ: HMAC-SHA256 от botToken с ключом "WebAppData"
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec botTokenKeySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(botTokenKeySpec);
            byte[] secretKey = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
            LOGGER.debug("🔍 Секретный ключ создан (длина: {} байт)", secretKey.length);
            
            // Вычисляем HMAC-SHA256 для dataCheckString с использованием секретного ключа
            mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            
            String calculatedHash = bytesToHex(hashBytes);
            LOGGER.debug("🔍 Вычисленный hash: {}", calculatedHash);
            LOGGER.debug("🔍 Сравнение хешей: ожидаемый={}, вычисленный={}", expectedHash, calculatedHash);
            
            boolean isValid = calculatedHash.equals(expectedHash);
            LOGGER.debug("🔍 Результат валидации hash: {}", isValid ? "✅ Валиден" : "❌ Невалиден");
            
            return isValid;

        } catch (Exception e) {
            LOGGER.error("❌ Ошибка валидации hash: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Упрощенная и исправленная версия validateSignature
     */
    private boolean validateSignature(Map<String, String> params, String expectedSignature, String botToken) {
        try {
            String dataCheckString = buildDataCheckString(params);
            
            LOGGER.debug("Data check string для signature: {}", dataCheckString);
            
            // Создаем секретный ключ (botToken как ключ, "WebAppData" как данные)
            Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec botTokenKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmacSha256.init(botTokenKeySpec);
            byte[] secretKey = hmacSha256.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));
            
            // Подписываем данные
            hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec dataKeySpec = new SecretKeySpec(secretKey, HMAC_SHA256);
            hmacSha256.init(dataKeySpec);
            byte[] signatureBytes = hmacSha256.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            
            // Только URL-safe Base64 без padding
            String calculatedSignature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            
            LOGGER.debug("Сравнение signature: ожидаемый={}, вычисленный={}", expectedSignature, calculatedSignature);
            
            return calculatedSignature.equals(expectedSignature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.error("❌ Ошибка вычисления signature: {}", e.getMessage(), e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public Long extractTelegramId(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            String userStr = params.get("user");
            if (userStr == null || userStr.isBlank()) {
                return null;
            }

            JsonNode userNode = objectMapper.readTree(userStr);
            JsonNode idNode = userNode.get("id");
            if (idNode == null || idNode.isNull()) {
                return null;
            }
            if (idNode.isNumber()) {
                return idNode.longValue();
            }
            if (idNode.isTextual()) {
                String text = idNode.asText().trim();
                if (text.isEmpty()) {
                    return null;
                }
                return Long.parseLong(text);
            }
            return Long.parseLong(idNode.asText().trim());

        } catch (Exception e) {
            LOGGER.error("❌ Ошибка извлечения telegram_id: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Извлекает start_param из initData
     * 
     * @param initData строка initData от Telegram
     * @return значение start_param или null если не найден
     */
    public String extractStartParam(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            String startParam = params.get("start_param");
            
            if (startParam != null && !startParam.isEmpty()) {
                LOGGER.debug("🔍 Извлечен start_param: {}", startParam);
                return startParam;
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка извлечения start_param: {}", e.getMessage(), e);
            return null;
        }
    }
}