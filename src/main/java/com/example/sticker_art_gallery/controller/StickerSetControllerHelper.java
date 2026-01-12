package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.StickerSetFilterRequest;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Вспомогательный класс для контроллеров стикерсетов
 */
@Component
public class StickerSetControllerHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetControllerHelper.class);
    private final UserService userService;
    
    @Autowired
    public StickerSetControllerHelper(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Извлечь ID текущего пользователя из SecurityContext (может вернуть null)
     */
    public Long getCurrentUserIdOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                LOGGER.debug("🔍 getCurrentUserIdOrNull: authentication is null or not authenticated");
                return null;
            }
            
            Object principal = authentication.getPrincipal();
            if (principal != null && "anonymousUser".equals(principal.toString())) {
                LOGGER.debug("🔍 getCurrentUserIdOrNull: principal is anonymousUser");
                return null;
            }
            
            String name = authentication.getName();
            LOGGER.debug("🔍 getCurrentUserIdOrNull: authentication.getName() = {}", name);
            
            if (name != null && !name.isEmpty()) {
                try {
                    Long userId = Long.valueOf(name);
                    LOGGER.debug("🔍 getCurrentUserIdOrNull: успешно извлечен userId = {}", userId);
                    return userId;
                } catch (NumberFormatException e) {
                    LOGGER.warn("⚠️ getCurrentUserIdOrNull: не удалось преобразовать '{}' в Long: {}", name, e.getMessage());
                    return null;
                }
            }
            
            LOGGER.debug("🔍 getCurrentUserIdOrNull: authentication.getName() is null or empty");
            return null;
        } catch (Exception e) {
            LOGGER.warn("⚠️ getCurrentUserIdOrNull: ошибка при извлечении userId: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Извлечь ID текущего пользователя (с исключением если не авторизован)
     */
    public Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        return userId;
    }

    /**
     * Локализованное сообщение об ошибке
     */
    public String languageResponse(String language, String ruMessage, String enMessage) {
        return "ru".equalsIgnoreCase(language) ? ruMessage : enMessage;
    }
    
    /**
     * Проверка, является ли пользователь владельцем или админом
     */
    public boolean isOwnerOrAdmin(Long ownerId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        
        // Если текущий пользователь является владельцем
        if (currentUserId.equals(ownerId)) {
            return true;
        }
        
        // Проверяем, является ли пользователь администратором
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            }
        } catch (Exception e) {
            LOGGER.warn("⚠️ Ошибка при проверке прав администратора: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Извлечь язык из заголовка X-Language или из initData пользователя
     * @param request HTTP запрос для получения заголовков
     * @return код языка (ru/en), по умолчанию "en"
     */
    public String getLanguageFromHeaderOrUser(HttpServletRequest request) {
        // Сначала проверяем заголовок X-Language
        String languageFromHeader = request.getHeader("X-Language");
        if (languageFromHeader != null && !languageFromHeader.trim().isEmpty()) {
            String lang = languageFromHeader.trim().toLowerCase();
            if ("ru".equals(lang) || "en".equals(lang)) {
                LOGGER.debug("🌐 Язык из заголовка X-Language: {}", lang);
                return lang;
            }
        }
        
        // Если заголовок не указан или некорректный, пытаемся получить из initData пользователя
        Long currentUserId = getCurrentUserIdOrNull();
        if (currentUserId != null) {
            try {
                java.util.Optional<UserEntity> userOpt = userService.findById(currentUserId);
                if (userOpt.isPresent()) {
                    String userLanguage = userOpt.get().getLanguageCode();
                    if (userLanguage != null && !userLanguage.trim().isEmpty()) {
                        String lang = userLanguage.trim().toLowerCase();
                        if ("ru".equals(lang) || "en".equals(lang)) {
                            LOGGER.debug("🌐 Язык из initData пользователя {}: {}", currentUserId, lang);
                            return lang;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("⚠️ Ошибка при получении языка пользователя {}: {}", currentUserId, e.getMessage());
            }
        }
        
        // По умолчанию возвращаем английский
        LOGGER.debug("🌐 Используется язык по умолчанию: en");
        return "en";
    }
    
    /**
     * Построение объекта фильтра из параметров HTTP запроса
     */
    public StickerSetFilterRequest buildFilter(
            int page, int size, String sort, String direction,
            String categoryKeys, StickerSetType type, boolean officialOnly, Long authorId,
            boolean hasAuthorOnly, Long userId, boolean likedOnly,
            boolean shortInfo, boolean preview, HttpServletRequest request) {
        
        StickerSetFilterRequest filter = new StickerSetFilterRequest();
        
        // PageRequest
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        pageRequest.setSort(sort);
        pageRequest.setDirection(direction);
        filter.setPageRequest(pageRequest);
        
        // Контекст
        filter.setLanguage(getLanguageFromHeaderOrUser(request));
        filter.setCurrentUserId(getCurrentUserIdOrNull());
        
        // Логика совместимости: если type указан явно - используем его
        // Если type не указан, но officialOnly=true - используем OFFICIAL
        // Иначе null (любые типы)
        StickerSetType effectiveType = type;
        if (effectiveType == null && officialOnly) {
            effectiveType = StickerSetType.OFFICIAL;
        }
        
        // Фильтры
        if (categoryKeys != null && !categoryKeys.trim().isEmpty()) {
            filter.setCategoryKeys(java.util.Set.of(categoryKeys.split(",")));
        }
        filter.setType(effectiveType);
        filter.setAuthorId(authorId);
        filter.setHasAuthorOnly(hasAuthorOnly);
        filter.setUserId(userId);
        filter.setLikedOnly(likedOnly);
        filter.setShortInfo(shortInfo);
        filter.setPreview(preview);
        
        return filter;
    }
}
