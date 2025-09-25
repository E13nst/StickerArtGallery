package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.dto.TelegramInitData;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.service.user.UserService;
import com.example.sticker_art_gallery.util.TelegramInitDataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Провайдер аутентификации для Telegram
 */
@Component
public class TelegramAuthenticationProvider implements AuthenticationProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramAuthenticationProvider.class);
    
    private final TelegramInitDataValidator validator;
    private final UserService userService;
    
    @Autowired
    public TelegramAuthenticationProvider(TelegramInitDataValidator validator, UserService userService) {
        this.validator = validator;
        this.userService = userService;
    }
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            LOGGER.debug("❌ Неподдерживаемый тип аутентификации: {}", authentication.getClass().getSimpleName());
            return null;
        }
        
        TelegramAuthenticationToken token = (TelegramAuthenticationToken) authentication;
        String initData = token.getInitData();
        Long telegramId = token.getTelegramId();
        String botName = token.getBotName();
        
        LOGGER.info("🔐 Аутентификация пользователя с telegram_id: {} для бота: {}", telegramId, botName);
        LOGGER.debug("🔍 Детали токена: initData length={}, telegramId={}, botName={}", 
                initData != null ? initData.length() : 0, telegramId, botName);
        
        try {
            // Валидируем initData для конкретного бота
            LOGGER.debug("🔍 Начинаем валидацию initData для telegram_id: {} и бота: {}", telegramId, botName);
            if (!validator.validateInitData(initData, botName)) {
                LOGGER.warn("❌ Невалидная initData для пользователя: {} и бота: {}", telegramId, botName);
                return null;
            }
            LOGGER.debug("✅ InitData валидна для telegram_id: {} и бота: {}", telegramId, botName);
            
            // Извлекаем данные пользователя из initData
            LOGGER.debug("🔍 Извлекаем данные пользователя из initData");
            TelegramInitData.TelegramUser telegramUser = extractTelegramUser(initData);
            if (telegramUser == null) {
                LOGGER.warn("❌ Не удалось извлечь данные пользователя из initData");
                return null;
            }
            LOGGER.debug("✅ Извлечены данные пользователя: id={}, username={}, firstName={}, lastName={}", 
                    telegramUser.getId(), telegramUser.getUsername(), telegramUser.getFirstName(), telegramUser.getLastName());
            
            // Находим или создаем пользователя
            LOGGER.debug("🔍 Ищем или создаем пользователя в базе данных");
            UserEntity user = userService.findOrCreateByTelegramId(
                    telegramUser.getId(),
                    telegramUser.getUsername(),
                    telegramUser.getFirstName(),
                    telegramUser.getLastName(),
                    telegramUser.getPhotoUrl()
            );
            LOGGER.debug("✅ Пользователь найден/создан: id={}, username={}, role={}", 
                    user.getId(), user.getUsername(), user.getRole());
            
            // Создаем authorities на основе роли пользователя
            LOGGER.debug("🔍 Создаем authorities для роли: {}", user.getRole());
            var authorities = TelegramAuthenticationToken.createAuthorities(user);
            LOGGER.debug("✅ Созданы authorities: {}", authorities);
            
            // Создаем аутентифицированный токен
            TelegramAuthenticationToken authenticatedToken = new TelegramAuthenticationToken(
                    user, initData, telegramId, botName, authorities
            );
            LOGGER.debug("✅ Создан аутентифицированный токен");
            
            LOGGER.info("✅ Пользователь успешно аутентифицирован: {} (роль: {}) для бота: {}", 
                    user.getUsername(), user.getRole(), botName);
            
            return authenticatedToken;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка аутентификации пользователя {} для бота {}: {}", telegramId, botName, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return TelegramAuthenticationToken.class.isAssignableFrom(authentication);
    }
    
    /**
     * Извлекает данные пользователя из initData
     * В реальном проекте лучше использовать JSON парсер
     */
    private TelegramInitData.TelegramUser extractTelegramUser(String initData) {
        try {
            // Простой парсинг для извлечения данных пользователя
            // В реальном проекте используйте ObjectMapper для парсинга JSON
            
            // Извлекаем telegram_id
            Long telegramId = validator.extractTelegramId(initData);
            if (telegramId == null) {
                return null;
            }
            
            // Простой парсинг остальных полей
            String username = extractValue(initData, "username");
            String firstName = extractValue(initData, "first_name");
            String lastName = extractValue(initData, "last_name");
            String photoUrl = extractValue(initData, "photo_url");
            
            return new TelegramInitData.TelegramUser(
                    telegramId,
                    false, // isBot
                    firstName,
                    lastName,
                    username,
                    null, // languageCode
                    null, // isPremium
                    null, // addedToAttachmentMenu
                    null, // allowsWriteToPm
                    photoUrl
            );
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка извлечения данных пользователя: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Простой метод извлечения значения из строки
     */
    private String extractValue(String initData, String fieldName) {
        try {
            int startIndex = initData.indexOf("\"" + fieldName + "\":\"");
            if (startIndex == -1) {
                return null;
            }
            
            startIndex = initData.indexOf("\"", startIndex + fieldName.length() + 3) + 1;
            int endIndex = initData.indexOf("\"", startIndex);
            
            if (endIndex == -1) {
                return null;
            }
            
            return initData.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
}
