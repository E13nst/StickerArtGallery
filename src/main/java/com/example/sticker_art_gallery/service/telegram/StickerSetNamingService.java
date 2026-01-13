package com.example.sticker_art_gallery.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Утилитный сервис для работы с именами стикерсетов.
 * Генерирует дефолтные имена и проверяет владение стикерсетами.
 */
@Component
public class StickerSetNamingService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetNamingService.class);
    
    private final String botUsername;
    
    @Autowired
    public StickerSetNamingService(
            @Value("${app.telegram.bot-username:stixlybot}") String botUsername) {
        this.botUsername = botUsername != null && !botUsername.isBlank() 
            ? botUsername.toLowerCase() 
            : "stixlybot";
        LOGGER.info("✅ StickerSetNamingService инициализирован с botUsername: {}", this.botUsername);
    }
    
    /**
     * Генерирует дефолтное имя стикерсета для пользователя.
     * 
     * @param userId Telegram ID пользователя
     * @param username username пользователя (может быть null)
     * @return имя стикерсета в формате {username}_by_{botUsername} или user_{userId}_by_{botUsername}
     */
    public String generateDefaultName(Long userId, String username) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        String prefix = (username != null && !username.isBlank()) 
            ? username.toLowerCase() 
            : "user_" + userId;
        
        String fullName = prefix + "_by_" + botUsername;
        LOGGER.debug("📝 Сгенерировано дефолтное имя стикерсета: {} (userId: {}, username: {})", 
                fullName, userId, username);
        return fullName;
    }
    
    /**
     * Проверяет, что стикерсет принадлежит нашему боту.
     * 
     * @param stickerSetName имя стикерсета
     * @return true если имя заканчивается на _by_{botUsername}
     */
    public boolean isOwnedByBot(String stickerSetName) {
        if (stickerSetName == null || stickerSetName.isBlank()) {
            return false;
        }
        
        String expectedSuffix = "_by_" + botUsername;
        boolean owned = stickerSetName.toLowerCase().endsWith(expectedSuffix);
        LOGGER.debug("🔍 Проверка владения стикерсетом '{}': {}", stickerSetName, owned);
        return owned;
    }
    
    /**
     * Валидирует имя стикерсета и проверяет владение.
     * 
     * @param name имя стикерсета
     * @throws IllegalArgumentException если имя пустое или стикерсет не принадлежит нашему боту
     */
    public void validateStickerSetName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Sticker set name cannot be empty");
        }
        
        if (!isOwnedByBot(name)) {
            throw new IllegalArgumentException(
                "You can only add stickers to sets created by this bot (ending with _by_" + botUsername + ")"
            );
        }
    }
    
    /**
     * Получает username бота
     */
    public String getBotUsername() {
        return botUsername;
    }
}
