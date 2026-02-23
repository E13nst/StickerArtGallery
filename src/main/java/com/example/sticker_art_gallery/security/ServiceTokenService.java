package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для управления сервисными токенами межсервисной авторизации.
 * Токены конфигурируются через app.service-api-token
 * и сравниваются по SHA-256 хэшу, чтобы избежать логирования/хранения в открытом виде.
 */
@Component
public class ServiceTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTokenService.class);
    private static final String HASH_ALGORITHM = "SHA-256";

    private final Map<String, byte[]> tokenHashes;

    public ServiceTokenService(AppConfig appConfig) {
        Map<String, byte[]> hashes = new HashMap<>();

        registerToken(hashes, "StickerBot", appConfig.getServiceApiToken());

        this.tokenHashes = Collections.unmodifiableMap(hashes);
    }

    /**
     * Находит имя сервиса по переданному токену.
     *
     * @param rawToken исходный токен из заголовка
     * @return имя сервиса, если токен валиден
     */
    public Optional<String> authenticate(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return Optional.empty();
        }

        if (tokenHashes.isEmpty()) {
            LOGGER.warn("⚠️ Нет зарегистрированных сервисных токенов для проверки.");
            return Optional.empty();
        }

        byte[] candidateHash = digest(rawToken.trim());
        return tokenHashes.entrySet().stream()
                .filter(entry -> MessageDigest.isEqual(candidateHash, entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean hasRegisteredTokens() {
        return !tokenHashes.isEmpty();
    }

    private void registerToken(Map<String, byte[]> hashes, String serviceName, String token) {
        if (token == null || token.trim().isEmpty()) {
            LOGGER.debug("🔐 Токен для сервиса {} не задан", serviceName);
            return;
        }
        byte[] digest = digest(token.trim());
        hashes.put(serviceName, digest);
        LOGGER.info("🔐 Зарегистрирован сервисный токен для сервиса {}", serviceName);
    }

    private byte[] digest(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            return messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hash algorithm " + HASH_ALGORITHM + " is not available", e);
        }
    }
}

