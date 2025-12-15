package com.example.sticker_art_gallery.dto.wallet;

import com.example.sticker_art_gallery.validation.ValidTonAddress;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO для запроса привязки кошелька
 */
public class LinkWalletRequest {

    @NotBlank(message = "Адрес кошелька не может быть пустым")
    @ValidTonAddress
    private String walletAddress;

    private String walletType;

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
}

