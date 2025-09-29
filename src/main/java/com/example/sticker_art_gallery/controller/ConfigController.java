package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Config", description = "API для получения конфигурации приложения")
public class ConfigController {

    private final AppConfig appConfig;

    @Autowired
    public ConfigController(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @GetMapping("/config")
    @Operation(
        summary = "Получить конфигурацию приложения",
        description = "Возвращает конфигурацию приложения, включая имя бота и URL мини-приложения"
    )
    @ApiResponse(responseCode = "200", description = "Конфигурация успешно получена")
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        
        // Получаем имя бота из токена (извлекаем username из токена)
        String botToken = appConfig.getTelegram().getBotToken();
        String botName = extractBotNameFromToken(botToken);
        
        config.put("botName", botName);
        config.put("miniAppUrl", appConfig.getMiniApp().getUrl());
        
        return config;
    }

    /**
     * Извлекает имя бота из конфигурации
     * Сначала проверяем переменную окружения TELEGRAM_BOT_USERNAME
     * Если не задана, используем значение по умолчанию
     */
    private String extractBotNameFromToken(String botToken) {
        // Проверяем переменную окружения
        String botUsername = System.getenv("TELEGRAM_BOT_USERNAME");
        if (botUsername != null && !botUsername.trim().isEmpty()) {
            return botUsername;
        }
        
        // Возвращаем значение по умолчанию
        return "StickerGallery";
    }
}
