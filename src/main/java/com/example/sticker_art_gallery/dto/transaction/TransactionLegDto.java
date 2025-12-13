package com.example.sticker_art_gallery.dto.transaction;

import com.example.sticker_art_gallery.model.transaction.LegType;

/**
 * DTO для части транзакции
 */
public class TransactionLegDto {

    private Long id;
    private LegType legType;
    private Long toEntityId;
    private String toWalletAddress;
    private Long amountNano;

    public TransactionLegDto() {
    }

    public TransactionLegDto(Long id, LegType legType, Long toEntityId, String toWalletAddress, Long amountNano) {
        this.id = id;
        this.legType = legType;
        this.toEntityId = toEntityId;
        this.toWalletAddress = toWalletAddress;
        this.amountNano = amountNano;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LegType getLegType() {
        return legType;
    }

    public void setLegType(LegType legType) {
        this.legType = legType;
    }

    public Long getToEntityId() {
        return toEntityId;
    }

    public void setToEntityId(Long toEntityId) {
        this.toEntityId = toEntityId;
    }

    public String getToWalletAddress() {
        return toWalletAddress;
    }

    public void setToWalletAddress(String toWalletAddress) {
        this.toWalletAddress = toWalletAddress;
    }

    public Long getAmountNano() {
        return amountNano;
    }

    public void setAmountNano(Long amountNano) {
        this.amountNano = amountNano;
    }
}

