package com.example.sticker_art_gallery.dto.payment;

import com.example.sticker_art_gallery.validation.ValidTonAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос создания TON Pay платежа за ART-пакет")
public class CreateTonPaymentRequest {

    @NotBlank(message = "Код пакета обязателен")
    @Schema(description = "Код ART-пакета", example = "BASIC")
    private String packageCode;

    @NotBlank(message = "Адрес отправителя обязателен")
    @ValidTonAddress
    @Schema(description = "Адрес подключенного TON-кошелька пользователя", example = "UQD...")
    private String senderAddress;

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }
}
