package com.example.sticker_art_gallery.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "TON Connect transaction payload для sendTransaction")
public class TonConnectTransactionDto {

    @Schema(description = "Unix timestamp, до которого транзакция валидна", example = "1777891200")
    private Long validUntil;

    @Schema(description = "Список сообщений TON Connect")
    private List<TonConnectMessageDto> messages;

    public TonConnectTransactionDto() {
    }

    public TonConnectTransactionDto(Long validUntil, List<TonConnectMessageDto> messages) {
        this.validUntil = validUntil;
        this.messages = messages;
    }

    public Long getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Long validUntil) {
        this.validUntil = validUntil;
    }

    public List<TonConnectMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<TonConnectMessageDto> messages) {
        this.messages = messages;
    }
}
