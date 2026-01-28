package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ответ с данными созданного invoice
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с данными созданного invoice для оплаты Stars")
public class CreateInvoiceResponse {

    @Schema(description = "URL для оплаты invoice", example = "https://t.me/invoice/...")
    private String invoiceUrl;

    @Schema(description = "ID намерения покупки", example = "123")
    private Long intentId;

    @Schema(description = "Информация о пакете")
    private StarsPackageDto starsPackage;
}
