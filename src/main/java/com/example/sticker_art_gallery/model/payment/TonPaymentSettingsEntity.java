package com.example.sticker_art_gallery.model.payment;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ton_payment_settings")
public class TonPaymentSettingsEntity {

    public static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "id")
    private Short id = SINGLETON_ID;

    @Column(name = "merchant_wallet_address", columnDefinition = "TEXT")
    private String merchantWalletAddress;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (id == null) {
            id = SINGLETON_ID;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (isEnabled == null) {
            isEnabled = Boolean.TRUE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }
    public String getMerchantWalletAddress() { return merchantWalletAddress; }
    public void setMerchantWalletAddress(String merchantWalletAddress) { this.merchantWalletAddress = merchantWalletAddress; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean enabled) { isEnabled = enabled; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
