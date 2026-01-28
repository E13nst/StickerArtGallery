package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ответ на обработку успешного платежа
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ на обработку успешного платежа")
public class ProcessPaymentResponse {

    @Schema(description = "Успешно ли обработан платеж", example = "true")
    private Boolean success;

    @Schema(description = "ID созданной покупки", example = "123")
    private Long purchaseId;

    @Schema(description = "Количество начисленных ART-баллов", example = "100")
    private Long artCredited;

    @Schema(description = "Сообщение об ошибке (если success=false)")
    private String errorMessage;

    public static ProcessPaymentResponse success(Long purchaseId, Long artCredited) {
        return new ProcessPaymentResponse(true, purchaseId, artCredited, null);
    }

    public static ProcessPaymentResponse failure(String errorMessage) {
        return new ProcessPaymentResponse(false, null, null, errorMessage);
    }
}
