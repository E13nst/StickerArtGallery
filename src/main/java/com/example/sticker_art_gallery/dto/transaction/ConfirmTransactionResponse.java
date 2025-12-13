package com.example.sticker_art_gallery.dto.transaction;

import com.example.sticker_art_gallery.model.transaction.IntentStatus;

/**
 * DTO для ответа на запрос подтверждения транзакции
 */
public class ConfirmTransactionResponse {

    private Long intentId;
    private IntentStatus status;
    private String txHash;
    private boolean success;
    private String message;

    public ConfirmTransactionResponse() {
    }

    public ConfirmTransactionResponse(Long intentId, IntentStatus status, String txHash, boolean success, String message) {
        this.intentId = intentId;
        this.status = status;
        this.txHash = txHash;
        this.success = success;
        this.message = message;
    }

    public Long getIntentId() {
        return intentId;
    }

    public void setIntentId(Long intentId) {
        this.intentId = intentId;
    }

    public IntentStatus getStatus() {
        return status;
    }

    public void setStatus(IntentStatus status) {
        this.status = status;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

