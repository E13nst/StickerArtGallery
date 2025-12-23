package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.dto.TelegramInitData;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.util.TelegramInitDataValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
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
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TelegramAuthenticationProvider(TelegramInitDataValidator validator, 
                                         UserProfileService userProfileService,
                                         ObjectMapper objectMapper) {
        this.validator = validator;
        this.userProfileService = userProfileService;
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
        
        LOGGER.info("🔐 Аутентификация пользователя с telegram_id: {}", telegramId);
        LOGGER.debug("🔍 Детали токена: initData length={}, telegramId={}", 
                initData != null ? initData.length() : 0, telegramId);
        
        try {
            // Валидируем initData
            LOGGER.debug("🔍 Начинаем валидацию initData для telegram_id: {}", telegramId);
            if (!validator.validateInitData(initData)) {
                LOGGER.warn("❌ Невалидная initData для пользователя: {}", telegramId);
                return null;
            }
            LOGGER.debug("✅ InitData валидна для telegram_id: {}", telegramId);
            
            // Извлекаем данные пользователя из initData
            LOGGER.debug("🔍 Извлекаем данные пользователя из initData");
            TelegramInitData.TelegramUser telegramUser = extractTelegramUser(initData);
            if (telegramUser == null) {
                LOGGER.warn("❌ Не удалось извлечь данные пользователя из initData");
                return null;
            }
            LOGGER.debug("✅ Извлечены данные пользователя: id={}, username={}, firstName={}, lastName={}", 
                    telegramUser.getId(), telegramUser.getUsername(), telegramUser.getFirstName(), telegramUser.getLastName());
            
            // Атомарно создаем или обновляем пользователя и его профиль в одной транзакции
            LOGGER.debug("🔍 Атомарно создаем/обновляем пользователя и профиль в базе данных");
            UserProfileEntity profile = userProfileService.ensureUserAndProfileExists(
                telegramUser.getId(),
                telegramUser.getFirstName(),
                telegramUser.getLastName(),
                telegramUser.getUsername(),
                telegramUser.getLanguageCode(),
                telegramUser.getIsPremium()
            );
            LOGGER.debug("✅ Пользователь и профиль найдены/созданы: userId={}, role={}, artBalance={}", 
                    profile.getUserId(), profile.getRole(), profile.getArtBalance());

            if (Boolean.TRUE.equals(profile.getIsBlocked())) {
                LOGGER.warn("❌ Пользователь {} заблокирован. Аутентификация отклонена.", telegramUser.getId());
                throw new DisabledException("User is blocked");
            }

            // Создаем authorities на основе роли профиля
            LOGGER.debug("🔍 Создаем authorities для роли: {}", profile.getRole());
            var authorities = TelegramAuthenticationToken.createAuthorities(profile.getRole().name());
            LOGGER.debug("✅ Созданы authorities: {}", authorities);
            
            // Создаем аутентифицированный токен
            TelegramAuthenticationToken authenticatedToken = new TelegramAuthenticationToken(
                    new AuthUserPrincipal(profile.getUserId(), profile.getRole()),
                    initData, telegramId, authorities
            );
            LOGGER.debug("✅ Создан аутентифицированный токен");
            
            LOGGER.info("✅ Пользователь успешно аутентифицирован: {} (роль: {})", 
                    telegramUser.getUsername(), profile.getRole());
            
            return authenticatedToken;
            
        } catch (DisabledException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка аутентификации пользователя {}: {}", telegramId, e.getMessage(), e);
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
