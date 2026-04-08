package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.config.AppConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Service Toggles API", description = "Просмотр текущих конфигурационных toggles сервиса")
public class ServiceTogglesController {

    private final AppConfig appConfig;

    public ServiceTogglesController(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @GetMapping("/toggles")
    @Operation(summary = "Получить текущие toggles сервиса")
    public ResponseEntity<Map<String, Object>> getToggles() {
        AppConfig.Telegram tg = appConfig.getTelegram();
        AppConfig.Webhook webhook = appConfig.getWebhook();
        AppConfig.StickerBot stickerBot = appConfig.getStickerbot();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("appUrl", appConfig.getUrl());

        Map<String, Object> telegram = new LinkedHashMap<>();
        telegram.put("webhookOwner", tg.getWebhookOwner());
        telegram.put("nativePaymentEnabled", tg.isNativePaymentEnabled());
        telegram.put("nativeMessagingEnabled", tg.isNativeMessagingEnabled());
        telegram.put("supportEnabled", tg.isSupportEnabled());
        telegram.put("supportUseTopics", tg.isSupportUseTopics());
        telegram.put("supportChatIdConfigured", tg.getSupportChatId() != null);
        telegram.put("webhookSecretTokenConfigured", tg.getWebhookSecretToken() != null && !tg.getWebhookSecretToken().isBlank());
        telegram.put("webhookUrl", tg.getWebhookUrl());
        telegram.put("webhookAutoRegisterOnStartup", tg.isWebhookAutoRegisterOnStartup());
        response.put("telegram", telegram);

        Map<String, Object> inboundWebhook = new LinkedHashMap<>();
        inboundWebhook.put("hmacEnforced", webhook.isHmacEnforced());
        inboundWebhook.put("hmacSecretConfigured", webhook.getHmacSecret() != null && !webhook.getHmacSecret().isBlank());
        response.put("webhookSecurity", inboundWebhook);

        Map<String, Object> legacyStickerBot = new LinkedHashMap<>();
        legacyStickerBot.put("apiUrlConfigured", stickerBot.getApiUrl() != null && !stickerBot.getApiUrl().isBlank());
        legacyStickerBot.put("serviceTokenConfigured", stickerBot.getServiceToken() != null && !stickerBot.getServiceToken().isBlank());
        response.put("legacyStickerBot", legacyStickerBot);

        return ResponseEntity.ok(response);
    }
}
