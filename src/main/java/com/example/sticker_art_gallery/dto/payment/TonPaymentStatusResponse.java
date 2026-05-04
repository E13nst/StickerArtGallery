package com.example.sticker_art_gallery.dto.payment;

import com.example.sticker_art_gallery.model.payment.TonPaymentIntentEntity;
import com.example.sticker_art_gallery.model.payment.TonPaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Статус TON Pay покупки ART")
public class TonPaymentStatusResponse {

    private Long intentId;
    private TonPaymentStatus status;
    private String packageCode;
    private Long amountNano;
    private String asset;
    private Long artAmount;
    private String reference;
    private String txHash;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TonPaymentStatusResponse fromEntity(TonPaymentIntentEntity intent, String txHash) {
        TonPaymentStatusResponse response = new TonPaymentStatusResponse();
        response.setIntentId(intent.getId());
        response.setStatus(intent.getStatus());
        response.setPackageCode(intent.getPackageCode());
        response.setAmountNano(intent.getExpectedAmountNano());
        response.setAsset(intent.getAsset());
        response.setArtAmount(intent.getArtAmount());
        response.setReference(intent.getReference());
        response.setTxHash(txHash);
        response.setFailureReason(intent.getFailureReason());
        response.setCreatedAt(intent.getCreatedAt());
        response.setUpdatedAt(intent.getUpdatedAt());
        return response;
    }

    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public TonPaymentStatus getStatus() { return status; }
    public void setStatus(TonPaymentStatus status) { this.status = status; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public Long getAmountNano() { return amountNano; }
    public void setAmountNano(Long amountNano) { this.amountNano = amountNano; }
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    public Long getArtAmount() { return artAmount; }
    public void setArtAmount(Long artAmount) { this.artAmount = artAmount; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
