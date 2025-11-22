package com.example.sticker_art_gallery.security;

import com.example.sticker_art_gallery.util.TelegramInitDataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import java.io.IOException;
import org.springframework.http.HttpStatus;

/**
 * Фильтр для аутентификации через Telegram initData
 */
@Component
public class TelegramAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramAuthenticationFilter.class);
    private static final String TELEGRAM_INIT_DATA_HEADER = "X-Telegram-Init-Data";
    
    private final TelegramInitDataValidator validator;
    private final TelegramAuthenticationProvider authenticationProvider;
    
    @Autowired
    public TelegramAuthenticationFilter(TelegramInitDataValidator validator, 
                                       TelegramAuthenticationProvider authenticationProvider) {
        this.validator = validator;
        this.authenticationProvider = authenticationProvider;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String initData = request.getHeader(TELEGRAM_INIT_DATA_HEADER);
        
        LOGGER.debug("🔍 TelegramAuthenticationFilter: Запрос к {} | InitData: {}", 
                request.getRequestURI(), 
                initData != null && !initData.trim().isEmpty() ? "present" : "null");
        
        if (initData != null && !initData.trim().isEmpty()) {
            LOGGER.info("🔍 Обнаружен заголовок X-Telegram-Init-Data. Используется настроенный токен бота");
            LOGGER.debug("🔍 InitData (первые 50 символов): {}", 
                    initData.length() > 50 ? initData.substring(0, 50) + "..." : initData);
            
            try {
                // Валидируем initData
                LOGGER.debug("🔍 Начинаем валидацию initData");
                if (!validator.validateInitData(initData)) {
                    LOGGER.warn("⚠️ InitData невалидна (возможно, устарела или повреждена), требуется обновление авторизации");
                    filterChain.doFilter(request, response);
                    return;
                }
                LOGGER.debug("✅ InitData валидна");
                
                // Извлекаем telegram_id из initData
                Long telegramId = validator.extractTelegramId(initData);
                LOGGER.debug("🔍 Извлечен telegram_id: {}", telegramId);
                
                if (telegramId != null) {
                    LOGGER.info("🔐 Попытка аутентификации для telegram_id: {}", telegramId);
                    
                    // Создаем неаутентифицированный токен
                    TelegramAuthenticationToken token = new TelegramAuthenticationToken(initData, telegramId);
                    LOGGER.debug("🔍 Создан TelegramAuthenticationToken для telegram_id: {}", telegramId);
                    
                    // Аутентифицируем токен
                    var authentication = authenticationProvider.authenticate(token);
                    LOGGER.debug("🔍 Результат аутентификации: {}", 
                            authentication != null ? authentication.isAuthenticated() : "null");
                    
                    if (authentication != null && authentication.isAuthenticated()) {
                        // Устанавливаем аутентификацию в контекст
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        LOGGER.info("✅ Аутентификация успешна для telegram_id: {}", telegramId);
                    } else {
                        LOGGER.warn("❌ Аутентификация не удалась для telegram_id: {}", telegramId);
                    }
                } else {
                    LOGGER.warn("❌ Не удалось извлечь telegram_id из initData");
                }
                
            } catch (DisabledException e) {
                LOGGER.warn("❌ Пользователь заблокирован. Доступ запрещен: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"User is blocked\"}");
                return;
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка обработки Telegram аутентификации: {}", e.getMessage(), e);
            }
        } else {
            LOGGER.debug("🔍 Заголовок X-Telegram-Init-Data отсутствует или пуст");
        }
        
        // Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Не фильтруем запросы к статическим ресурсам и некоторым системным эндпоинтам
        return path.startsWith("/actuator/") || 
               path.startsWith("/error") ||
               path.equals("/") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/mini-app/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs");
    }
}
