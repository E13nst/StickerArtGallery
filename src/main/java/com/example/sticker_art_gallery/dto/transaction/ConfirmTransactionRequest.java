package com.example.sticker_art_gallery.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO для запроса подтверждения транзакции
 */
public class ConfirmTransactionRequest {

    @NotNull(message = "ID намерения не может быть null")
    @Positive(message = "ID намерения должен быть положительным числом")
    private Long intentId;

    @NotBlank(message = "Хеш транзакции не может быть пустым")
    private String txHash;

    @NotBlank(message = "Адрес кошелька отправителя не может быть пустым")
    private String fromWallet;

    public Long getIntentId() {
        return intentId;
    }

    public void setIntentId(Long intentId) {
        this.intentId = intentId;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getFromWallet() {
        return fromWallet;
    }

    public void setFromWallet(String fromWallet) {
        this.fromWallet = fromWallet;
    }
}

