package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Запрос на валидацию платежа (pre_checkout_query)
 */
@Getter
@Setter
@Schema(description = "Запрос на валидацию платежа перед оплатой")
public class ValidatePaymentRequest {

    @NotBlank(message = "Invoice payload обязателен")
    @Schema(description = "Payload invoice для связи с заказом", required = true)
    private String invoicePayload;

    @NotNull(message = "User ID обязателен")
    @Schema(description = "ID пользователя Telegram", required = true, example = "123456789")
    private Long userId;

    @NotNull(message = "Total amount обязателен")
    @Schema(description = "Общая сумма платежа в Stars", required = true, example = "50")
    private Integer totalAmount;
}
