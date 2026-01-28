package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ответ на валидацию платежа
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ на валидацию платежа")
public class ValidatePaymentResponse {

    @Schema(description = "Валиден ли платеж", example = "true")
    private Boolean valid;

    @Schema(description = "Сообщение об ошибке (если valid=false)", example = "Invalid payment amount")
    private String errorMessage;

    public static ValidatePaymentResponse valid() {
        return new ValidatePaymentResponse(true, null);
    }

    public static ValidatePaymentResponse invalid(String errorMessage) {
        return new ValidatePaymentResponse(false, errorMessage);
    }
}
