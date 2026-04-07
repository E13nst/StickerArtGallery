package com.example.sticker_art_gallery.controller.internal;

import com.example.sticker_art_gallery.service.telegram.TelegramWebhookOwnershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal Admin API для управления Telegram webhook ownership.
 * Используется при cutover (переключение StickerBot → Java) и rollback.
 *
 * Защита: X-Service-Token (ServiceTokenAuthenticationFilter) + роль INTERNAL.
 */
@RestController
@RequestMapping("/api/internal/telegram/ownership")
@PreAuthorize("hasRole('INTERNAL')")
@Tag(name = "Telegram Ownership Admin API",
        description = "Управление владельцем Telegram webhook (cutover / rollback). Только для внутреннего использования.")
public class TelegramOwnershipAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramOwnershipAdminController.class);

    private final TelegramWebhookOwnershipService ownershipService;

    public TelegramOwnershipAdminController(TelegramWebhookOwnershipService ownershipService) {
        this.ownershipService = ownershipService;
    }

    /**
     * Проверить текущий webhook — smoke-тест для мониторинга.
     */
    @GetMapping("/status")
    @Operation(summary = "Проверить текущий Telegram webhook (getWebhookInfo + getMe)")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            String botUsername = ownershipService.getBotUsername();
            TelegramWebhookOwnershipService.WebhookInfo info = ownershipService.getWebhookInfo();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("botUsername", botUsername);
            result.put("webhookUrl", info.url());
            result.put("pendingUpdateCount", info.pendingUpdateCount());
            result.put("lastErrorMessage", info.lastErrorMessage());
            result.put("isJavaOwner", ownershipService.isJavaOwner());

            LOGGER.info("📊 Webhook status: bot=@{}, url={}, pending={}, javaOwner={}",
                    botUsername, info.url(), info.pendingUpdateCount(), ownershipService.isJavaOwner());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка получения webhook status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Шаг 1 cutover: удалить текущий webhook (отключить StickerBot как получателя).
     * После этого шага Telegram не доставляет updates никому до регистрации нового webhook.
     */
    @DeleteMapping("/webhook")
    @Operation(summary = "Удалить текущий webhook (deleteWebhook) — шаг 1 cutover")
    public ResponseEntity<Map<String, Object>> deleteWebhook() {
        LOGGER.warn("🔄 CUTOVER STEP 1: deleteWebhook запущен");
        try {
            ownershipService.deleteWebhook();
            return ResponseEntity.ok(Map.of("ok", true, "message", "Webhook удалён"));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка deleteWebhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    /**
     * Шаг 2 cutover: зарегистрировать webhook на Java URL.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Зарегистрировать webhook на Java (setWebhook) — шаг 2 cutover")
    public ResponseEntity<Map<String, Object>> setWebhook(@RequestBody SetWebhookRequest request) {
        LOGGER.warn("🔄 CUTOVER STEP 2: setWebhook → {}", request.webhookUrl());
        try {
            List<String> allowedUpdates = request.allowedUpdates() != null
                    ? request.allowedUpdates()
                    : List.of("message", "pre_checkout_query", "successful_payment",
                            "callback_query", "inline_query", "chosen_inline_result");

            ownershipService.setWebhook(request.webhookUrl(), request.secretToken(), allowedUpdates);

            TelegramWebhookOwnershipService.WebhookInfo info = ownershipService.getWebhookInfo();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("webhookUrl", info.url());
            result.put("pendingUpdateCount", info.pendingUpdateCount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка setWebhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    public record SetWebhookRequest(
            String webhookUrl,
            String secretToken,
            List<String> allowedUpdates
    ) {}
}
