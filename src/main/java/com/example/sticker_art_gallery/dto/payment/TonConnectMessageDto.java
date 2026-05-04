package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "TON Connect message для отправки через кошелёк")
public class TonConnectMessageDto {

    @Schema(description = "Адрес получателя", example = "EQD...")
    private String address;

    @Schema(description = "Сумма в nanoTON строкой", example = "100000000")
    private String amount;

    @Schema(description = "Base64 payload сообщения")
    private String payload;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
