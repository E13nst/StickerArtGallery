package com.example.sticker_art_gallery.model.payment;

import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ton_purchases")
public class TonPurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intent_id", nullable = false)
    private TonPaymentIntentEntity intent;

    @Column(name = "intent_id", nullable = false, insertable = false, updatable = false)
    private Long intentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private StarsPackageEntity starsPackage;

    @Column(name = "package_id", insertable = false, updatable = false)
    private Long packageId;

    @Column(name = "package_code", nullable = false, length = 64)
    private String packageCode;

    @Column(name = "ton_paid_nano", nullable = false)
    private Long tonPaidNano;

    @Column(name = "asset", nullable = false, length = 128)
    private String asset = "TON";

    @Column(name = "art_credited", nullable = false)
    private Long artCredited;

    @Column(name = "reference", nullable = false, unique = true, length = 255)
    private String reference;

    @Column(name = "body_base64_hash", unique = true, length = 255)
    private String bodyBase64Hash;

    @Column(name = "tx_hash", nullable = false, unique = true, columnDefinition = "TEXT")
    private String txHash;

    @Column(name = "sender_address", nullable = false, columnDefinition = "TEXT")
    private String senderAddress;

    @Column(name = "recipient_address", nullable = false, columnDefinition = "TEXT")
    private String recipientAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "art_transaction_id")
    private ArtTransactionEntity artTransaction;

    @Column(name = "art_transaction_id", insertable = false, updatable = false)
    private Long artTransactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (asset == null) {
            asset = "TON";
        }
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
    public TonPaymentIntentEntity getIntent() { return intent; }
    public void setIntent(TonPaymentIntentEntity intent) {
        this.intent = intent;
        this.intentId = intent != null ? intent.getId() : null;
    }
    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public StarsPackageEntity getStarsPackage() { return starsPackage; }
    public void setStarsPackage(StarsPackageEntity starsPackage) {
        this.starsPackage = starsPackage;
        this.packageId = starsPackage != null ? starsPackage.getId() : null;
    }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public Long getTonPaidNano() { return tonPaidNano; }
    public void setTonPaidNano(Long tonPaidNano) { this.tonPaidNano = tonPaidNano; }
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    public Long getArtCredited() { return artCredited; }
    public void setArtCredited(Long artCredited) { this.artCredited = artCredited; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getBodyBase64Hash() { return bodyBase64Hash; }
    public void setBodyBase64Hash(String bodyBase64Hash) { this.bodyBase64Hash = bodyBase64Hash; }
    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }
    public String getSenderAddress() { return senderAddress; }
    public void setSenderAddress(String senderAddress) { this.senderAddress = senderAddress; }
    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }
    public ArtTransactionEntity getArtTransaction() { return artTransaction; }
    public void setArtTransaction(ArtTransactionEntity artTransaction) {
        this.artTransaction = artTransaction;
        this.artTransactionId = artTransaction != null ? artTransaction.getId() : null;
    }
    public Long getArtTransactionId() { return artTransactionId; }
    public void setArtTransactionId(Long artTransactionId) { this.artTransactionId = artTransactionId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
