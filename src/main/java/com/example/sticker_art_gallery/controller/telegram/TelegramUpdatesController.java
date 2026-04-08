package com.example.sticker_art_gallery.controller.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.service.telegram.UpdateRouterService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
public class TelegramUpdatesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramUpdatesController.class);
    private static final String TELEGRAM_SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final UpdateRouterService updateRouterService;
    private final AppConfig appConfig;

    public TelegramUpdatesController(UpdateRouterService updateRouterService, AppConfig appConfig) {
        this.updateRouterService = updateRouterService;
        this.appConfig = appConfig;
    }

    @PostMapping("/updates")
    public ResponseEntity<Map<String, Object>> receiveUpdates(@RequestBody JsonNode update, HttpServletRequest request) {
        if (!verifySecretToken(request)) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "error", "Forbidden"));
        }
        try {
            updateRouterService.routeUpdate(update);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            LOGGER.error("Telegram update handling failed: {}", e.getMessage(), e);
            // Telegram ожидает 200. Ошибки логируем, но возвращаем OK, чтобы не зациклить ретраи.
            return ResponseEntity.ok(Map.of("ok", true));
        }
    }

    private boolean verifySecretToken(HttpServletRequest request) {
        String expected = appConfig.getTelegram().getWebhookSecretToken();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        String actual = request.getHeader(TELEGRAM_SECRET_HEADER);
        boolean valid = expected.equals(actual);
        if (!valid) {
            LOGGER.warn("Telegram webhook secret token mismatch");
        }
        return valid;
    }
}
