package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Регистрирует Telegram webhook при старте сервиса.
 * Работает только если app.telegram.webhook-owner=java и авто-регистрация включена.
 */
@Component
public class TelegramWebhookStartupRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramWebhookStartupRegistrar.class);

    private final AppConfig appConfig;
    private final TelegramWebhookOwnershipService ownershipService;

    public TelegramWebhookStartupRegistrar(AppConfig appConfig, TelegramWebhookOwnershipService ownershipService) {
        this.appConfig = appConfig;
        this.ownershipService = ownershipService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhookOnStartup() {
        AppConfig.Telegram telegram = appConfig.getTelegram();
        if (telegram == null) {
            LOGGER.warn("⚠️ Telegram config is missing, webhook startup registration skipped");
            return;
        }
        if (!"java".equalsIgnoreCase(telegram.getWebhookOwner())) {
            LOGGER.info("ℹ️ Telegram webhook owner is '{}', startup auto-registration skipped", telegram.getWebhookOwner());
            return;
        }
        if (!telegram.isWebhookAutoRegisterOnStartup()) {
            LOGGER.info("ℹ️ Startup auto-registration disabled by app.telegram.webhook-auto-register-on-startup=false");
            return;
        }

        String webhookUrl = resolveWebhookUrl(telegram);
        if (webhookUrl == null) {
            LOGGER.error("❌ Cannot resolve webhook URL on startup. Set app.telegram.webhook-url or app.url");
            return;
        }

        String secretToken = telegram.getWebhookSecretToken();
        List<String> allowedUpdates = List.of(
                "message",
                "pre_checkout_query",
                "callback_query",
                "inline_query",
                "chosen_inline_result",
                "web_app_query"
        );

        try {
            ownershipService.setWebhook(webhookUrl, secretToken, allowedUpdates);
            TelegramWebhookOwnershipService.WebhookInfo info = ownershipService.getWebhookInfo();
            LOGGER.info("✅ Startup webhook registration success: webhookUrl={}, pendingUpdateCount={}",
                    info.url(), info.pendingUpdateCount());
            if (info.lastErrorMessage() != null && !info.lastErrorMessage().isBlank()) {
                LOGGER.warn("⚠️ Telegram webhook lastErrorMessage after startup registration: {}", info.lastErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("❌ Startup webhook registration failed: {}", e.getMessage(), e);
        }
    }

    private String resolveWebhookUrl(AppConfig.Telegram telegram) {
        if (telegram.getWebhookUrl() != null && !telegram.getWebhookUrl().isBlank()) {
            return telegram.getWebhookUrl().trim();
        }
        String appUrl = appConfig.getUrl();
        if (appUrl == null || appUrl.isBlank()) {
            return null;
        }
        return appUrl.replaceAll("/+$", "") + "/api/telegram/updates";
    }
}
