package com.example.sticker_art_gallery.model.payment;

import com.example.sticker_art_gallery.model.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ton_payment_intents")
public class TonPaymentIntentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private StarsPackageEntity starsPackage;

    @Column(name = "package_id", nullable = false, insertable = false, updatable = false)
    private Long packageId;

    @Column(name = "package_code", nullable = false, length = 64)
    private String packageCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TonPaymentStatus status = TonPaymentStatus.CREATED;

    @Column(name = "expected_amount_nano", nullable = false)
    private Long expectedAmountNano;

    @Column(name = "asset", nullable = false, length = 128)
    private String asset = "TON";

    @Column(name = "art_amount", nullable = false)
    private Long artAmount;

    @Column(name = "sender_address", nullable = false, columnDefinition = "TEXT")
    private String senderAddress;

    @Column(name = "recipient_address", nullable = false, columnDefinition = "TEXT")
    private String recipientAddress;

    @Column(name = "reference", unique = true, length = 255)
    private String reference;

    @Column(name = "body_base64_hash", unique = true, length = 255)
    private String bodyBase64Hash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ton_connect_message")
    private String tonConnectMessage;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (asset == null) {
            asset = "TON";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public StarsPackageEntity getStarsPackage() { return starsPackage; }
    public void setStarsPackage(StarsPackageEntity starsPackage) {
        this.starsPackage = starsPackage;
        this.packageId = starsPackage != null ? starsPackage.getId() : null;
    }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public TonPaymentStatus getStatus() { return status; }
    public void setStatus(TonPaymentStatus status) { this.status = status; }
    public Long getExpectedAmountNano() { return expectedAmountNano; }
    public void setExpectedAmountNano(Long expectedAmountNano) { this.expectedAmountNano = expectedAmountNano; }
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    public Long getArtAmount() { return artAmount; }
    public void setArtAmount(Long artAmount) { this.artAmount = artAmount; }
    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }
    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getBodyBase64Hash() { return bodyBase64Hash; }
    public void setBodyBase64Hash(String bodyBase64Hash) { this.bodyBase64Hash = bodyBase64Hash; }
    public String getTonConnectMessage() { return tonConnectMessage; }
    public void setTonConnectMessage(String tonConnectMessage) { this.tonConnectMessage = tonConnectMessage; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
