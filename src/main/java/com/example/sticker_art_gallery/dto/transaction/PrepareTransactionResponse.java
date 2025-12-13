package com.example.sticker_art_gallery.dto.transaction;

import com.example.sticker_art_gallery.model.transaction.IntentStatus;
import com.example.sticker_art_gallery.model.transaction.IntentType;

import java.util.List;

/**
 * DTO для ответа на запрос подготовки транзакции
 */
public class PrepareTransactionResponse {

    private Long intentId;
    private IntentType intentType;
    private IntentStatus status;
    private Long amountNano;
    private String currency;
    private List<TransactionLegDto> legs;

    public PrepareTransactionResponse() {
    }

    public PrepareTransactionResponse(Long intentId, IntentType intentType, IntentStatus status, 
                                     Long amountNano, String currency, List<TransactionLegDto> legs) {
        this.intentId = intentId;
        this.intentType = intentType;
        this.status = status;
        this.amountNano = amountNano;
        this.currency = currency;
        this.legs = legs;
    }

    public Long getIntentId() {
        return intentId;
    }

    public void setIntentId(Long intentId) {
        this.intentId = intentId;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }

    public IntentStatus getStatus() {
        return status;
    }

    public void setStatus(IntentStatus status) {
        this.status = status;
    }

    public Long getAmountNano() {
        return amountNano;
    }

    public void setAmountNano(Long amountNano) {
        this.amountNano = amountNano;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<TransactionLegDto> getLegs() {
        return legs;
    }

    public void setLegs(List<TransactionLegDto> legs) {
        this.legs = legs;
    }
}

