package com.example.sticker_art_gallery.config;

import com.example.sticker_art_gallery.security.ServiceTokenAuthenticationFilter;
import com.example.sticker_art_gallery.security.TelegramAuthenticationFilter;
import com.example.sticker_art_gallery.security.TelegramAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * Конфигурация Spring Security
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final TelegramAuthenticationFilter telegramAuthenticationFilter;
    private final ServiceTokenAuthenticationFilter serviceTokenAuthenticationFilter;
    private final TelegramAuthenticationProvider telegramAuthenticationProvider;
    
    @Autowired
    public SecurityConfig(TelegramAuthenticationFilter telegramAuthenticationFilter,
                         ServiceTokenAuthenticationFilter serviceTokenAuthenticationFilter,
                         TelegramAuthenticationProvider telegramAuthenticationProvider) {
        this.telegramAuthenticationFilter = telegramAuthenticationFilter;
        this.serviceTokenAuthenticationFilter = serviceTokenAuthenticationFilter;
        this.telegramAuthenticationProvider = telegramAuthenticationProvider;
    }
    
    @Bean
    MvcRequestMatcher.Builder mvcRequestMatcherBuilder(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
            // Отключаем CSRF для API
            .csrf(csrf -> csrf.disable())
            
            // Настройка CORS
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(java.util.List.of("*"));
                corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(java.util.List.of("*"));
                corsConfig.setAllowCredentials(true);
                return corsConfig;
            }))
            
            // Настройка сессий
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Добавляем наши кастомные фильтры
            .addFilterBefore(serviceTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(telegramAuthenticationFilter, ServiceTokenAuthenticationFilter.class)
            
            // Настройка провайдера аутентификации
            .authenticationProvider(telegramAuthenticationProvider)
            
            // Разрешаем iframe для Telegram Mini App
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable()) // Отключаем X-Frame-Options для Telegram
            )
            
            // Настройка авторизации
            .authorizeHttpRequests(authz -> authz
                // Публичные эндпоинты (но фильтр все равно применяется)
                .requestMatchers(mvc.pattern("/actuator/**")).permitAll()
                .requestMatchers(mvc.pattern("/error")).permitAll()
                .requestMatchers(mvc.pattern("/")).permitAll()
                .requestMatchers(mvc.pattern("/static/**")).permitAll()
                .requestMatchers(mvc.pattern("/css/**")).permitAll()
                .requestMatchers(mvc.pattern("/js/**")).permitAll()
                .requestMatchers(mvc.pattern("/images/**")).permitAll()
                .requestMatchers(mvc.pattern("/mini-app/**")).permitAll()
                .requestMatchers(mvc.pattern("/mini-app/index.html")).permitAll()
                .requestMatchers(mvc.pattern("/mini-app/app.js")).permitAll()
                .requestMatchers(mvc.pattern("/mini-app/style.css")).permitAll()
                .requestMatchers(mvc.pattern("/mini-app/test.html")).permitAll()
                
                // Swagger UI и OpenAPI
                .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
                .requestMatchers(mvc.pattern("/swagger-ui.html")).permitAll()
                .requestMatchers(mvc.pattern("/v3/api-docs/**")).permitAll()
                .requestMatchers(mvc.pattern("/swagger-resources/**")).permitAll()
                .requestMatchers(mvc.pattern("/webjars/**")).permitAll()
                
                // Dev эндпоинты (только в dev профиле)
                .requestMatchers(mvc.pattern("/dev/**")).permitAll()
                
                // Auth эндпоинты (фильтр применяется, но аутентификация не требуется)
                .requestMatchers(mvc.pattern("/api/auth/**")).permitAll()
                
                // API прокси стикеров - временно запрещаем доступ к stickers
                .requestMatchers(mvc.pattern("/api/proxy/stickers/**")).denyAll()
                .requestMatchers(mvc.pattern("/api/proxy/**")).permitAll()

                // API стикерсетов - публичный доступ для тестирования
                .requestMatchers(mvc.pattern("/api/stickersets/**")).permitAll()
                
                // API для авторизованных пользователей (USER или ADMIN)
                .requestMatchers(mvc.pattern("/api/users/**")).hasAnyRole("USER", "ADMIN")
                .requestMatchers(mvc.pattern("/api/profiles/**")).hasAnyRole("USER", "ADMIN")
                
                // Межсервисные эндпоинты
                .requestMatchers(mvc.pattern("/internal/**")).hasRole("INTERNAL")
                
                // API только для ADMIN
                .requestMatchers(mvc.pattern("/api/bots/**")).hasRole("ADMIN")
                
                // Все остальные запросы разрешены (временно для отладки)
                .anyRequest().permitAll()
            )
            
            // Настройка обработки ошибок
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                })
            );
        
        return http.build();
    }
}
