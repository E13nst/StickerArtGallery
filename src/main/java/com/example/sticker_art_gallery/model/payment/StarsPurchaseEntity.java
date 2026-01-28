package com.example.sticker_art_gallery.model.payment;

import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Entity истории покупок ART за Telegram Stars
 */
@Entity
@Table(name = "stars_purchases")
public class StarsPurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_intent_id")
    private StarsInvoiceIntentEntity invoiceIntent;

    @Column(name = "invoice_intent_id", insertable = false, updatable = false)
    private Long invoiceIntentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private StarsPackageEntity starsPackage;

    @Column(name = "package_id", insertable = false, updatable = false)
    private Long packageId;

    @Column(name = "package_code", nullable = false, length = 64)
    private String packageCode;

    @Column(name = "stars_paid", nullable = false)
    private Integer starsPaid;

    @Column(name = "art_credited", nullable = false)
    private Long artCredited;

    @Column(name = "telegram_payment_id", unique = true, length = 255)
    private String telegramPaymentId;

    @Column(name = "telegram_charge_id", unique = true, length = 255)
    private String telegramChargeId;

    @Column(name = "invoice_payload", columnDefinition = "TEXT")
    private String invoicePayload;

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
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public StarsInvoiceIntentEntity getInvoiceIntent() {
        return invoiceIntent;
    }

    public void setInvoiceIntent(StarsInvoiceIntentEntity invoiceIntent) {
        this.invoiceIntent = invoiceIntent;
        this.invoiceIntentId = invoiceIntent != null ? invoiceIntent.getId() : null;
    }

    public Long getInvoiceIntentId() {
        return invoiceIntentId;
    }

    public void setInvoiceIntentId(Long invoiceIntentId) {
        this.invoiceIntentId = invoiceIntentId;
    }

    public StarsPackageEntity getStarsPackage() {
        return starsPackage;
    }

    public void setStarsPackage(StarsPackageEntity starsPackage) {
        this.starsPackage = starsPackage;
        this.packageId = starsPackage != null ? starsPackage.getId() : null;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public Integer getStarsPaid() {
        return starsPaid;
    }

    public void setStarsPaid(Integer starsPaid) {
        this.starsPaid = starsPaid;
    }

    public Long getArtCredited() {
        return artCredited;
    }

    public void setArtCredited(Long artCredited) {
        this.artCredited = artCredited;
    }

    public String getTelegramPaymentId() {
        return telegramPaymentId;
    }

    public void setTelegramPaymentId(String telegramPaymentId) {
        this.telegramPaymentId = telegramPaymentId;
    }

    public String getTelegramChargeId() {
        return telegramChargeId;
    }

    public void setTelegramChargeId(String telegramChargeId) {
        this.telegramChargeId = telegramChargeId;
    }

    public String getInvoicePayload() {
        return invoicePayload;
    }

    public void setInvoicePayload(String invoicePayload) {
        this.invoicePayload = invoicePayload;
    }

    public ArtTransactionEntity getArtTransaction() {
        return artTransaction;
    }

    public void setArtTransaction(ArtTransactionEntity artTransaction) {
        this.artTransaction = artTransaction;
        this.artTransactionId = artTransaction != null ? artTransaction.getId() : null;
    }

    public Long getArtTransactionId() {
        return artTransactionId;
    }

    public void setArtTransactionId(Long artTransactionId) {
        this.artTransactionId = artTransactionId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
