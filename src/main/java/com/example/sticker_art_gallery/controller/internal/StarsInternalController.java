package com.example.sticker_art_gallery.controller.internal;

import com.example.sticker_art_gallery.dto.payment.ProcessPaymentRequest;
import com.example.sticker_art_gallery.dto.payment.ProcessPaymentResponse;
import com.example.sticker_art_gallery.dto.payment.ValidatePaymentRequest;
import com.example.sticker_art_gallery.dto.payment.ValidatePaymentResponse;
import com.example.sticker_art_gallery.service.payment.StarsPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API контроллер для обработки платежей Stars (вызывается из Python бота)
 */
@RestController
@RequestMapping("/api/internal/stars")
@PreAuthorize("hasRole('INTERNAL')")
@Tag(name = "Internal Stars API", description = "Internal API для обработки платежей Stars (только для внутренних сервисов)")
public class StarsInternalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarsInternalController.class);

    private final StarsPaymentService starsPaymentService;

    @Autowired
    public StarsInternalController(StarsPaymentService starsPaymentService) {
        this.starsPaymentService = starsPaymentService;
    }

    /**
     * Валидация платежа перед оплатой (pre_checkout_query)
     */
    @PostMapping("/validate-payment")
    @Operation(
            summary = "Валидация платежа перед оплатой",
            description = "Валидирует платеж перед оплатой. Вызывается из Python бота при получении pre_checkout_query."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Валидация выполнена",
                    content = @Content(
                            schema = @Schema(implementation = ValidatePaymentResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {
                                      "valid": true
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "401", description = "Неверный service token"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<ValidatePaymentResponse> validatePayment(
            @Valid @RequestBody ValidatePaymentRequest request) {
        try {
            LOGGER.info("🔍 Валидация платежа: payload={}, userId={}, totalAmount={}",
                    request.getInvoicePayload(), request.getUserId(), request.getTotalAmount());

            ValidatePaymentResponse response = starsPaymentService.validatePreCheckout(
                    request.getInvoicePayload(),
                    request.getUserId(),
                    request.getTotalAmount()
            );

            if (response.getValid()) {
                LOGGER.info("✅ Платеж валиден");
                return ResponseEntity.ok(response);
            } else {
                LOGGER.warn("❌ Платеж невалиден: {}", response.getErrorMessage());
                return ResponseEntity.ok(response); // Возвращаем 200, но с valid=false
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при валидации платежа: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ValidatePaymentResponse.invalid("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Обработка успешного платежа
     */
    @PostMapping("/process-payment")
    @Operation(
            summary = "Обработка успешного платежа",
            description = "Обрабатывает успешный платеж и начисляет ART пользователю. Вызывается из Python бота при получении successful_payment."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Платеж обработан",
                    content = @Content(
                            schema = @Schema(implementation = ProcessPaymentResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                                    {
                                      "success": true,
                                      "purchaseId": 123,
                                      "artCredited": 100
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Неверный запрос"),
            @ApiResponse(responseCode = "401", description = "Неверный service token"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<ProcessPaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        try {
            LOGGER.info("💰 Обработка платежа: paymentId={}, chargeId={}, payload={}, userId={}",
                    request.getTelegramPaymentId(),
                    request.getTelegramChargeId(),
                    request.getInvoicePayload(),
                    request.getUserId());

            ProcessPaymentResponse response = starsPaymentService.processSuccessfulPayment(
                    request.getTelegramPaymentId(),
                    request.getTelegramChargeId(),
                    request.getInvoicePayload(),
                    request.getUserId()
            );

            if (response.getSuccess()) {
                LOGGER.info("✅ Платеж успешно обработан: purchaseId={}, artCredited={}",
                        response.getPurchaseId(), response.getArtCredited());
                return ResponseEntity.ok(response);
            } else {
                LOGGER.error("❌ Ошибка обработки платежа: {}", response.getErrorMessage());
                return ResponseEntity.ok(response); // Возвращаем 200, но с success=false
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.warn("⚠️ Ошибка валидации при обработке платежа: {}", e.getMessage());
            return ResponseEntity.ok(ProcessPaymentResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обработке платежа: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ProcessPaymentResponse.failure("Внутренняя ошибка сервера"));
        }
    }
}
