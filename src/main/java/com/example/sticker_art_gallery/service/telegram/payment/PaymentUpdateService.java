package com.example.sticker_art_gallery.service.telegram.payment;

import com.example.sticker_art_gallery.dto.payment.ProcessPaymentResponse;
import com.example.sticker_art_gallery.dto.payment.TelegramWebhookRequest;
import com.example.sticker_art_gallery.service.payment.StarsPaymentService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentUpdateService.class);

    private final TelegramBotApiService telegramBotApiService;
    private final StarsPaymentService starsPaymentService;

    public PaymentUpdateService(TelegramBotApiService telegramBotApiService,
                                StarsPaymentService starsPaymentService) {
        this.telegramBotApiService = telegramBotApiService;
        this.starsPaymentService = starsPaymentService;
    }

    public void handlePreCheckoutQuery(JsonNode preCheckoutQuery) {
        String queryId = preCheckoutQuery.path("id").asText(null);
        if (queryId == null || queryId.isBlank()) {
            return;
        }
        try {
            // Для wave1 подтверждаем pre-checkout и оставляем основную валидацию в successful_payment.
            telegramBotApiService.answerPreCheckoutQuery(queryId, true, null);
            LOGGER.info("PreCheckout approved: queryId={}", queryId);
        } catch (Exception e) {
            LOGGER.error("PreCheckout failed: {}", e.getMessage(), e);
            try {
                telegramBotApiService.answerPreCheckoutQuery(queryId, false, "Ошибка оплаты. Повторите позже.");
            } catch (Exception ignore) {
                LOGGER.warn("Failed to send pre-checkout error response: {}", ignore.getMessage());
            }
        }
    }

    public void handleSuccessfulPaymentMessage(JsonNode message) {
        JsonNode payment = message.path("successful_payment");
        if (payment.isMissingNode()) {
            return;
        }
        long userId = message.path("from").path("id").asLong(0);
        long chatId = message.path("chat").path("id").asLong(0);
        if (userId <= 0 || chatId == 0) {
            LOGGER.warn("Skipping successful_payment update with invalid user/chat ids");
            return;
        }

        try {
            TelegramWebhookRequest request = new TelegramWebhookRequest();
            request.setEvent("telegram_stars_payment_succeeded");
            request.setUserId(userId);
            request.setAmountStars(payment.path("total_amount").asInt());
            request.setCurrency(payment.path("currency").asText("XTR"));
            request.setTelegramChargeId(payment.path("telegram_payment_charge_id").asText());
            request.setInvoicePayload(payment.path("invoice_payload").asText());
            request.setTimestamp(System.currentTimeMillis() / 1000);

            ProcessPaymentResponse response = starsPaymentService.processWebhookPayment(request);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                telegramBotApiService.sendMessage(
                        chatId,
                        "✅ Оплата успешно завершена!\n\nНачислено ART: " + response.getArtCredited(),
                        null,
                        null,
                        null,
                        null
                );
            } else {
                telegramBotApiService.sendMessage(
                        chatId,
                        "⚠️ Оплата получена, но возникла ошибка обработки: " + response.getErrorMessage(),
                        null,
                        null,
                        null,
                        null
                );
            }
        } catch (Exception e) {
            LOGGER.error("Error processing successful_payment: {}", e.getMessage(), e);
            telegramBotApiService.sendMessage(
                    chatId,
                    "⚠️ Оплата получена, но обработка пока не завершена. Поддержка поможет, если ART не начислится.",
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
