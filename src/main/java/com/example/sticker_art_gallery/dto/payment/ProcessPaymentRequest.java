package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Запрос на обработку успешного платежа
 */
@Getter
@Setter
@Schema(description = "Запрос на обработку успешного платежа")
public class ProcessPaymentRequest {

    @NotBlank(message = "Telegram payment ID обязателен")
    @Schema(description = "ID платежа от Telegram", required = true)
    private String telegramPaymentId;

    @NotBlank(message = "Telegram charge ID обязателен")
    @Schema(description = "ID транзакции Stars от Telegram", required = true)
    private String telegramChargeId;

    @NotBlank(message = "Invoice payload обязателен")
    @Schema(description = "Payload invoice для связи с заказом", required = true)
    private String invoicePayload;

    @NotNull(message = "User ID обязателен")
    @Schema(description = "ID пользователя Telegram", required = true, example = "123456789")
    private Long userId;
}
