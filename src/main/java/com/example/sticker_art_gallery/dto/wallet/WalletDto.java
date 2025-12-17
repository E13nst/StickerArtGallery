package com.example.sticker_art_gallery.dto.wallet;

import java.time.OffsetDateTime;

/**
 * DTO для представления кошелька пользователя
 */
public class WalletDto {

    private Long id;
    private String walletAddress;
    private String walletType;
    private Boolean isActive;
    private OffsetDateTime createdAt;

    public WalletDto() {
    }

    public WalletDto(Long id, String walletAddress, String walletType, Boolean isActive, OffsetDateTime createdAt) {
        this.id = id;
        this.walletAddress = walletAddress;
        this.walletType = walletType;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getWalletType() {
        return walletType;
    }

    public void setWalletType(String walletType) {
        this.walletType = walletType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}





