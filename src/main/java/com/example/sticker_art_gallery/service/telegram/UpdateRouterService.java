package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.service.telegram.chat.StickerGalleryFlowService;
import com.example.sticker_art_gallery.service.telegram.inline.InlineGenerationService;
import com.example.sticker_art_gallery.service.telegram.inline.InlineQueryService;
import com.example.sticker_art_gallery.service.telegram.payment.PaymentUpdateService;
import com.example.sticker_art_gallery.service.telegram.support.SupportBridgeService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UpdateRouterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRouterService.class);

    private final TelegramUpdateDedupService dedupService;
    private final StickerGalleryFlowService stickerGalleryFlowService;
    private final SupportBridgeService supportBridgeService;
    private final PaymentUpdateService paymentUpdateService;
    private final InlineQueryService inlineQueryService;
    private final InlineGenerationService inlineGenerationService;

    public UpdateRouterService(TelegramUpdateDedupService dedupService,
                               StickerGalleryFlowService stickerGalleryFlowService,
                               SupportBridgeService supportBridgeService,
                               PaymentUpdateService paymentUpdateService,
                               InlineQueryService inlineQueryService,
                               InlineGenerationService inlineGenerationService) {
        this.dedupService = dedupService;
        this.stickerGalleryFlowService = stickerGalleryFlowService;
        this.supportBridgeService = supportBridgeService;
        this.paymentUpdateService = paymentUpdateService;
        this.inlineQueryService = inlineQueryService;
        this.inlineGenerationService = inlineGenerationService;
    }

    public void routeUpdate(JsonNode update) {
        long updateId = update.path("update_id").asLong(-1);
        if (updateId > 0 && dedupService.isDuplicate(updateId)) {
            LOGGER.debug("Duplicate update skipped: updateId={}", updateId);
            return;
        }

        if (update.has("pre_checkout_query")) {
            paymentUpdateService.handlePreCheckoutQuery(update.path("pre_checkout_query"));
            return;
        }

        if (update.has("inline_query")) {
            inlineQueryService.handleInlineQuery(update.path("inline_query"));
            return;
        }

        if (update.has("web_app_query")) {
            inlineGenerationService.handleWebAppQuery(update.path("web_app_query"));
            return;
        }

        if (update.has("callback_query")) {
            JsonNode callbackQuery = update.path("callback_query");
            if (supportBridgeService.handleSupportCallback(callbackQuery)) {
                return;
            }
            if (stickerGalleryFlowService.handleCallback(callbackQuery)) {
                return;
            }
            if (inlineGenerationService.handleGenerationCallback(callbackQuery)) {
                return;
            }
            return;
        }

        if (update.has("message")) {
            JsonNode message = update.path("message");

            // Двусторонний support bridge: operator -> user
            if (supportBridgeService.handleSupportReply(message)) {
                return;
            }

            // Payments from Telegram message.successful_payment
            if (message.has("successful_payment")) {
                paymentUpdateService.handleSuccessfulPaymentMessage(message);
                return;
            }

            // Команды
            if (message.hasNonNull("text")) {
                String text = message.path("text").asText("");
                long chatId = message.path("chat").path("id").asLong();
                long userId = message.path("from").path("id").asLong();
                switch (text) {
                    case "/start" -> {
                        stickerGalleryFlowService.handleStart(message);
                        return;
                    }
                    case "/help" -> {
                        stickerGalleryFlowService.handleHelp(message);
                        return;
                    }
                    case "/cancel" -> {
                        stickerGalleryFlowService.handleCancel(message);
                        return;
                    }
                    case "/support" -> {
                        supportBridgeService.handleSupportCommand(chatId, userId);
                        return;
                    }
                    default -> {
                        // no-op
                    }
                }
            }

            // User -> support forwarding when user selected support topic
            if (supportBridgeService.handleUserMessage(message)) {
                return;
            }

            // Incoming sticker -> add_to_gallery flow
            if (message.has("sticker")) {
                stickerGalleryFlowService.handleIncomingSticker(message);
            }
        }
    }
}
