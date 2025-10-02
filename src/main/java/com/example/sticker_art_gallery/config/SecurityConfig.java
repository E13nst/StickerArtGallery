package com.example.sticker_art_gallery.config;

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

/**
 * Конфигурация Spring Security
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final TelegramAuthenticationFilter telegramAuthenticationFilter;
    private final TelegramAuthenticationProvider telegramAuthenticationProvider;
    
    @Autowired
    public SecurityConfig(TelegramAuthenticationFilter telegramAuthenticationFilter,
                         TelegramAuthenticationProvider telegramAuthenticationProvider) {
        this.telegramAuthenticationFilter = telegramAuthenticationFilter;
        this.telegramAuthenticationProvider = telegramAuthenticationProvider;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
            
            // Добавляем наш кастомный фильтр
            .addFilterBefore(telegramAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Настройка провайдера аутентификации
            .authenticationProvider(telegramAuthenticationProvider)
            
            // Разрешаем iframe для Telegram Mini App
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable()) // Отключаем X-Frame-Options для Telegram
            )
            
            // Настройка авторизации
            .authorizeHttpRequests(authz -> authz
                // Публичные эндпоинты (но фильтр все равно применяется)
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/static/**").permitAll()
                .requestMatchers("/css/**").permitAll()
                .requestMatchers("/js/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/mini-app/**").permitAll()
                .requestMatchers("/mini-app/index.html").permitAll()
                .requestMatchers("/mini-app/app.js").permitAll()
                .requestMatchers("/mini-app/style.css").permitAll()
                .requestMatchers("/mini-app/test.html").permitAll()
                
                // Swagger UI и OpenAPI
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                
                // Dev эндпоинты (только в dev профиле)
                .requestMatchers("/dev/**").permitAll()
                
                // Auth эндпоинты (фильтр применяется, но аутентификация не требуется)
                .requestMatchers("/auth/**").permitAll()
                
                // API стикерсетов - публичный доступ для тестирования
                .requestMatchers("/api/stickersets/**").permitAll()
                
                // API для авторизованных пользователей (USER или ADMIN)
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                
                // API только для ADMIN
                .requestMatchers("/api/bots/**").hasRole("ADMIN")
                
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
