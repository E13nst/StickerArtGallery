package com.example.sticker_art_gallery.controller.internal;

import com.example.sticker_art_gallery.dto.payment.ProcessPaymentResponse;
import com.example.sticker_art_gallery.dto.payment.TelegramWebhookRequest;
import com.example.sticker_art_gallery.security.WebhookHmacVerifier;
import com.example.sticker_art_gallery.service.payment.PaymentShadowValidationService;
import com.example.sticker_art_gallery.service.payment.StarsPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal API контроллер для обработки webhook'ов платежей от StickerBot API.
 * Авторизация: X-Service-Token (проверяется ServiceTokenAuthenticationFilter)
 */
@RestController
@RequestMapping("/api/internal/webhooks")
@PreAuthorize("hasRole('INTERNAL')")
@Tag(name = "Internal Webhooks API", description = "Internal API для обработки webhook'ов от StickerBot API (авторизация через X-Service-Token)")
public class StarsInternalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarsInternalController.class);

    private final StarsPaymentService starsPaymentService;
    private final ObjectMapper objectMapper;
    private final WebhookHmacVerifier webhookHmacVerifier;
    private final PaymentShadowValidationService shadowValidationService;

    @Autowired
    public StarsInternalController(StarsPaymentService starsPaymentService,
                                   ObjectMapper objectMapper,
                                   WebhookHmacVerifier webhookHmacVerifier,
                                   PaymentShadowValidationService shadowValidationService) {
        this.starsPaymentService = starsPaymentService;
        this.objectMapper = objectMapper;
        this.webhookHmacVerifier = webhookHmacVerifier;
        this.shadowValidationService = shadowValidationService;
    }

    /**
     * Webhook от StickerBot API о платеже Telegram Stars
     */
    @PostMapping("/stars-payment")
    @Operation(
            summary = "Webhook от StickerBot API о платеже Telegram Stars",
            description = "Принимает уведомление от StickerBot API о successful_payment. Авторизация через X-Service-Token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Платеж обработан успешно",
                    content = @Content(
                            schema = @Schema(implementation = ProcessPaymentResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {
                                      "success": true,
                                      "purchaseId": 123,
                                      "artCredited": 100,
                                      "errorMessage": null
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Неверный формат запроса"),
            @ApiResponse(responseCode = "401", description = "Невалидный service token"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> handleTelegramWebhook(@RequestBody String requestBody,
                                                    HttpServletRequest httpRequest) {

        LOGGER.info("📨 Получен webhook от StickerBot API");

        // HMAC проверка (dual-check security: X-Service-Token + HMAC)
        String signature = httpRequest.getHeader("X-Webhook-Signature");
        WebhookHmacVerifier.VerificationResult hmacResult = webhookHmacVerifier.verify(requestBody, signature);
        if (!hmacResult.isAllowed()) {
            LOGGER.warn("🔒 Webhook отклонён: HMAC проверка не пройдена. Причина: {}", hmacResult.getReason());
            return ResponseEntity.status(401)
                    .body("{\"error\":\"Unauthorized\",\"message\":\"" + hmacResult.getReason() + "\"}");
        }
        if (hmacResult.isSignaturePresent() && hmacResult.isSignatureValid()) {
            LOGGER.debug("✅ HMAC подпись верна");
        } else if (!hmacResult.isSignaturePresent()) {
            LOGGER.debug("ℹ️ HMAC подпись отсутствует, проверка не применяется (hmac-enforced=false)");
        }

        try {
            // Парсинг JSON → DTO
            TelegramWebhookRequest request;
            try {
                request = objectMapper.readValue(requestBody, TelegramWebhookRequest.class);
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка парсинга JSON: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body("{\"error\":\"Invalid JSON format\",\"message\":\"" + e.getMessage() + "\"}");
            }

            LOGGER.info("🔍 Webhook данные: event={}, userId={}, chargeId={}, amountStars={}",
                    request.getEvent(), request.getUserId(), request.getTelegramChargeId(), request.getAmountStars());

            // Production-обработка платежа
            ProcessPaymentResponse response = starsPaymentService.processWebhookPayment(request);

            // Shadow-сверка (не влияет на ответ)
            try {
                List<String> mismatches = shadowValidationService.validate(request);
                if (!mismatches.isEmpty()) {
                    LOGGER.warn("⚠️ Shadow-сверка выявила {} расхождений для chargeId={}",
                            mismatches.size(), request.getTelegramChargeId());
                }
            } catch (Exception shadowEx) {
                LOGGER.warn("⚠️ Ошибка shadow-сверки (не влияет на ответ): {}", shadowEx.getMessage());
            }

            if (response.getSuccess()) {
                LOGGER.info("✅ Webhook платеж успешно обработан: purchaseId={}, artCredited={}",
                        response.getPurchaseId(), response.getArtCredited());
                return ResponseEntity.ok(response);
            } else {
                LOGGER.error("❌ Ошибка обработки webhook платежа: {}", response.getErrorMessage());
                return ResponseEntity.ok(response);
            }

        } catch (IllegalArgumentException e) {
            LOGGER.warn("⚠️ Ошибка валидации: {}", e.getMessage());
            return ResponseEntity.ok(ProcessPaymentResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("❌ Внутренняя ошибка при обработке webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ProcessPaymentResponse.failure("Внутренняя ошибка сервера"));
        }
    }
}
