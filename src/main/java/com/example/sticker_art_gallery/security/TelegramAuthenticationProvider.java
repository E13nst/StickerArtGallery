package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.dto.TelegramInitData;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.user.UserService;
import com.example.sticker_art_gallery.util.TelegramInitDataValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Провайдер аутентификации для Telegram
 */
@Component
public class TelegramAuthenticationProvider implements AuthenticationProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramAuthenticationProvider.class);
    
    private final TelegramInitDataValidator validator;
    private final UserProfileService userProfileService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TelegramAuthenticationProvider(TelegramInitDataValidator validator, 
                                         UserProfileService userProfileService,
                                         UserService userService,
                                         ObjectMapper objectMapper) {
        this.validator = validator;
        this.userProfileService = userProfileService;
        this.userService = userService;
        this.objectMapper = objectMapper;
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
            
            // Создаем или обновляем пользователя из данных Telegram
            userService.upsertFromTelegramData(
                telegramUser.getId(),
                telegramUser.getFirstName(),
                telegramUser.getLastName(),
                telegramUser.getUsername(),
                telegramUser.getLanguageCode(),
                telegramUser.getIsPremium()
            );
            
            // Создаем или получаем профиль пользователя (лениво)
            LOGGER.debug("🔍 Ищем или создаем профиль пользователя в базе данных");
            UserProfileEntity profile = userProfileService.getOrCreateDefault(telegramUser.getId());
            LOGGER.debug("✅ Профиль найден/создан: userId={}, role={}, artBalance={}", 
                    profile.getUserId(), profile.getRole(), profile.getArtBalance());

            // Создаем authorities на основе роли профиля
            LOGGER.debug("🔍 Создаем authorities для роли: {}", profile.getRole());
            var authorities = TelegramAuthenticationToken.createAuthorities(profile.getRole().name());
            LOGGER.debug("✅ Созданы authorities: {}", authorities);
            
            // Создаем аутентифицированный токен
            TelegramAuthenticationToken authenticatedToken = new TelegramAuthenticationToken(
                    new AuthUserPrincipal(profile.getUserId(), profile.getRole()),
                    initData, telegramId, botName, authorities
            );
            LOGGER.debug("✅ Создан аутентифицированный токен");
            
            LOGGER.info("✅ Пользователь успешно аутентифицирован: {} (роль: {}) для бота: {}", 
                    telegramUser.getUsername(), profile.getRole(), botName);
            
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
     * Извлекает данные пользователя из initData используя ObjectMapper
     */
    private TelegramInitData.TelegramUser extractTelegramUser(String initData) {
        try {
            // Извлекаем параметр user из initData
            String userParam = null;
            String[] params = initData.split("&");
            for (String param : params) {
                if (param.startsWith("user=")) {
                    userParam = param.substring(5); // Убираем "user="
                    break;
                }
            }
            
            if (userParam == null) {
                LOGGER.warn("⚠️ Параметр 'user' не найден в initData");
                return null;
            }
            
            // Декодируем URL-encoded JSON
            String userJson = URLDecoder.decode(userParam, StandardCharsets.UTF_8);
            LOGGER.debug("🔍 Распарсенный user JSON: {}", userJson);
            
            // Парсим JSON с помощью ObjectMapper
            TelegramInitData.TelegramUser telegramUser = objectMapper.readValue(userJson, TelegramInitData.TelegramUser.class);
            
            LOGGER.debug("✅ Пользователь извлечен: id={}, username={}, firstName={}, isPremium={}", 
                    telegramUser.getId(), telegramUser.getUsername(), telegramUser.getFirstName(), telegramUser.getIsPremium());
            
            return telegramUser;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка извлечения данных пользователя: {}", e.getMessage(), e);
            return null;
        }
    }
}
