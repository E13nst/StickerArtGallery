package com.example.sticker_art_gallery.dto.payment;

import com.example.sticker_art_gallery.model.payment.TonPaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ создания TON Pay платежа")
public class CreateTonPaymentResponse {

    private Long intentId;
    private TonPaymentStatus status;
    private String reference;
    private String bodyBase64Hash;
    private Long amountNano;
    private String asset;
    private String recipientAddress;
    private StarsPackageDto starsPackage;
    private TonConnectTransactionDto message;

    public CreateTonPaymentResponse() {
    }

    public CreateTonPaymentResponse(Long intentId,
                                    TonPaymentStatus status,
                                    String reference,
                                    String bodyBase64Hash,
                                    Long amountNano,
                                    String asset,
                                    String recipientAddress,
                                    StarsPackageDto starsPackage,
                                    TonConnectTransactionDto message) {
        this.intentId = intentId;
        this.status = status;
        this.reference = reference;
        this.bodyBase64Hash = bodyBase64Hash;
        this.amountNano = amountNano;
        this.asset = asset;
        this.recipientAddress = recipientAddress;
        this.starsPackage = starsPackage;
        this.message = message;
    }

    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public TonPaymentStatus getStatus() { return status; }
    public void setStatus(TonPaymentStatus status) { this.status = status; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getBodyBase64Hash() { return bodyBase64Hash; }
    public void setBodyBase64Hash(String bodyBase64Hash) { this.bodyBase64Hash = bodyBase64Hash; }
    public Long getAmountNano() { return amountNano; }
    public void setAmountNano(Long amountNano) { this.amountNano = amountNano; }
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }
    public StarsPackageDto getStarsPackage() { return starsPackage; }
    public void setStarsPackage(StarsPackageDto starsPackage) { this.starsPackage = starsPackage; }
    public TonConnectTransactionDto getMessage() { return message; }
    public void setMessage(TonConnectTransactionDto message) { this.message = message; }
}
